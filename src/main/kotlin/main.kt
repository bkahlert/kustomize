import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.colorize
import com.bkahlert.koodies.unit.Size.Companion.toSize
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.sources.ChainedValueSource
import com.github.ajalt.clikt.sources.ValueSource
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.cli.Config4kValueSource
import com.imgcstmzr.cli.EmptyValueSource
import com.imgcstmzr.cli.Env
import com.imgcstmzr.cli.Options.os
import com.imgcstmzr.guestfish.Guestfish
import com.imgcstmzr.guestfish.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.process.Downloader
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.applyLogging
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.listFilesRecursively
import com.imgcstmzr.util.readAll
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.system.exitProcess

val debug = true

fun main() {
//    CliCommand().subcommands(ImgCommand(), CstmzrCommand(), FlshCommand()).main(listOf("--help"))
    CliCommand().subcommands(ImgCommand(), CstmzrCommand(), FlshCommand()).main(
        listOf(
            "--config", "bother-you.conf", "img", "--reuse-last-working-copy",
            "cstmzr", "--username-rename=pi:bkahlert"
        )
    )
//    CliCommand().subcommands(XyzCommands(), ImgCommand(), FlshCommand()).main(listOf("img", "--name", "name-by-cmdline"))
//    CliCommand().subcommands(XyzCommands(), ImgCommand(), FlshCommand()).main(listOf("img"))
//    F1().subcommands(DriverCommands(), CircuitCommands(), FlshCommand()).main(args)
}


class CliCommand : NoOpCliktCommand(
    epilog = "((ε(*･ω･)_/${ANSI.termColors.colorize("ﾟ･.*･･｡☆")}",
    name = "imgcstmzr",
    allowMultipleSubcommands = true,
    printHelpOnEmptyArgs = true,
    help = "Downloads and Customizes Raspberry Pi Images"
) {
    private val valueSources = mutableListOf<ValueSource>(EmptyValueSource())

    init {
        context {
            autoEnvvarPrefix = "IMGCSTMZR"
            helpFormatter = ColorHelpFormatter()
            Paths.WORKING_DIRECTORY.listFilesRecursively()
            valueSource = ChainedValueSource(valueSources)
        }
    }

    private val configFile: Path? by option("--config", "--config-file", help = "configuration to be used for image customization")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val envFile: Path by option("--env", "--env-file", help = ".env file that can be used to pass credentials like a new user password")
        .path(mustExist = false, canBeDir = false, mustBeReadable = false).default(Paths.USER_HOME.resolve(".env.imgcstmzr"))

    private val name: String? by option(help = "name of the generated appliance").default("my-img")

    private val cacheDir: Path by option("--cache-dir", help = "temporary directory that holds intermediary states to improve performance")
        .path(canBeFile = false, mustBeWritable = true)
        .default(Cache.DEFAULT)

    private val shared by findOrSetObject { mutableMapOf<KClass<*>, Any>() }

    override fun run() {
        BlockRenderingLogger<Any?>("ImgCstmzr").applyLogging {
            configFile?.apply {
                valueSources.add(Config4kValueSource.from(this))
                logLine { "Using config (file: $this, name: $name, size: ${readAll().length})".cyan().bold() }
            }
            logLine { "Checking $envFile for .env file" }
            shared[Env::class] = Env(this, envFile)
            shared[Cache::class] = Cache(cacheDir)
            logLine { "Using cache (path: $cacheDir)" }
        }
    }
}

class ImgCommand : CliktCommand(help = "Provides an IMG file containing the specified OS") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "image" to listOf("img"),
    )

    private val downloader = Downloader()
    private val name by option().default("my-img")
    private val os by option().os().default(OperatingSystems.RaspberryPiLite)
    private val reuseLastWorkingCopy: Boolean by option().flag(default = false)
    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache
        val logger = BlockRenderingLogger<Any>("ImgCommand")
        shared[Path::class] =
            cache.provideCopy(name, reuseLastWorkingCopy, logger) {
                with(downloader) {
                    os.download(logger).also { TermUi.echo("Download completed.") }
                }
            }
    }
}

class CstmzrCommand : CliktCommand(help = "Customizes the given IMG") {

    override fun aliases(): Map<String, List<String>> = mapOf(
        "customize" to listOf("cstmzr"),
        "cstmz" to listOf("cstmzr"),
    )

    private val name by option().prompt(default = "my-img")

    private val os by option().enum<OperatingSystems>().default(OperatingSystems.RaspberryPiLite)
    private val size by option().convert { it.toSize() }
    private val enableSsh by option().convert { SshEnablementPatch() }
    private val usernameRename by option(help = "Format: [old-username]:[new-username], whereas both usernames must not be blank")
        .convert { it.split(":").map { it.trim() } }
        .convert { it.first() to it.last() }
        .convert { (oldUsername, newUsername) -> UsernamePatch(oldUsername, newUsername) }
    private val passwordChange by option(help = "Format: [username]:[new-password], whereas username and new password must not be blank")
        .convert { it.split(":").map { it.trim() } }
        .convert { it.first() to it.last() }
        .convert { (username, newPassword) -> PasswordPatch(username, newPassword) }
    private val usbOtgProfiles by option()
        .convert { it.split(",").map { it.trim() } }
        .convert { UsbOnTheGoPatch(it) }

    private val scripts: Map<String, String> by option().associate()

    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    val img by option()
        .file(canBeDir = false, mustBeReadable = true)
        .defaultLazy { (shared[Path::class] as Path).toFile() }

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache

        with(os based img.toPath()) {
            TermUi.debug(os)
            TermUi.debug(size)
            TermUi.debug(enableSsh)
            TermUi.debug(usernameRename)
            TermUi.debug(passwordChange)
            TermUi.debug(usbOtgProfiles)

//            patch.patch(img)
//            patcher(this, listOfNotNull(enableSsh, usernameRename, passwordChange, usbOtgProfiles))

            exitProcess(0)

            val logger = BlockRenderingLogger<Any>("Project: $fullName")

            val guestfish = Guestfish(this, logger, "$fullName-guestfish")
            guestfish.run(copyOutCommands(listOf("/etc/hostname", "/boot/cmdline.txt", "/boot/config.txt").map(Path::of)))
            guestfish.guestRootOnHost
                .also { echo("Success $it") }

            exitProcess(0)
//            val runtime = Runtime(name)
//            size?.let { os.increaseDiskSpace(size!!, this) }
//            runtime.bootAndRun("test", this, os.login("pi", "raspberry"), os.sequence("test", "wget https://bkahlert.com/wp-content/uploads/2019/08/Retrospective-02-Flipchart-Featured-720x405.jpg"))
//            scripts.forEach { (scriptName, script) ->
//                runtime.bootAndRun(scriptName, os, this, *os.compileSetupScript(scriptName, script))
        }
    }
}


class FlshCommand : CliktCommand(help = "Flashes the given IMG to an SD card") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "flash" to listOf("flsh"),
    )

    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    val img by option()
        .file(canBeDir = false, mustBeReadable = true)
        .defaultLazy { shared[File::class] as File }

    override fun run() {
        if (TermUi.confirm("Flashing to xxx?") == true) {
            TermUi.echo("OK!")
        }
    }
}
