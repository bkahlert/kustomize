import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.colorize
import com.bkahlert.koodies.unit.bytes
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
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.cli.Config4kValueSource
import com.imgcstmzr.cli.Config4kValueSource.Companion.update
import com.imgcstmzr.cli.Env
import com.imgcstmzr.cli.Options.os
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.SshPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.process.Downloader.download
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.debug
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


@OptIn(ExperimentalValueSourceApi::class)
class CliCommand : NoOpCliktCommand(
    epilog = "((ε(*･ω･)_/${ANSI.termColors.colorize("ﾟ･.*･･｡☆")}",
    name = "imgcstmzr",
    allowMultipleSubcommands = true,
    printHelpOnEmptyArgs = true,
    help = "Downloads and Customizes Raspberry Pi Images"
) {
    init {
        context {
            helpFormatter = ColorHelpFormatter()
            valueSource = Config4kValueSource.proxy()
        }
    }

    private val configFile: File? by option("--config", "--config-file", help = "configuration to be used for image customization")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val envFile: File by option("--env", "--env-file", help = ".env file that can be used to pass credentials like a new user password")
        .file(mustExist = false, canBeDir = false, mustBeReadable = false).default(Paths.USER_HOME.resolve(".env.imgcstmzr").toFile())

    private val name: String? by option(help = "name of the generated appliance").default("my-img")

    private val cacheDir: File by option("--cache-dir", help = "temporary directory that holds intermediary states to improve performance")
        .file(canBeFile = false, mustBeWritable = true)
        .default(Cache.DEFAULT.toFile())

    private val shared by findOrSetObject { mutableMapOf<KClass<*>, Any>() }

    override fun run() {
        configFile?.also {
            currentContext.valueSource.update(it)
            echo((ANSI.termColors.cyan + ANSI.termColors.bold)("Using config (file: $it, name: $name, size: ${it.readText().length})"))
        }
        echo("Checking $envFile for .env file")
        shared[Env::class] = Env(envFile.toPath())
        shared[Cache::class] = Cache(cacheDir.toPath())
        echo("Using cache (path: $cacheDir)")
    }
}

class ImgCommand : CliktCommand(help = "Provides an IMG file containing the specified OS") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "image" to listOf("img"),
    )

    private val name by option().default("my-img")
    private val os by option().os().default(OperatingSystems.RaspberryPiLite)
    private val reuseLastWorkingCopy: Boolean by option().flag(default = false)
    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache

        shared[Path::class] = cache.provideCopy(name, reuseLastWorkingCopy) { os.download().also { TermUi.echo("Download completed.") } }
    }
}

class CstmzrCommand : CliktCommand(help = "Customizes the given IMG") {

    override fun aliases(): Map<String, List<String>> = mapOf(
        "customize" to listOf("cstmzr"),
        "cstmz" to listOf("cstmzr"),
    )

    private val name by option().prompt(default = "my-img")

    private val os by option().os().default(OperatingSystems.RaspberryPiLite)
    private val size by option().convert { it.toLong().bytes }
    private val enableSsh by option().convert { SshPatch() }
    private val usernameRename by option().convert { argument: String ->
        argument.split(":").let { list: List<String> ->
            UsernamePatch(list[0], list[1])
        }
    }
    private val passwordChange by option().convert { argument: String ->
        argument.split(":").let { x ->
            PasswordPatch(x[0], x[1])
        }
    }
    private val usbOtgProfiles by option().convert { UsbOnTheGoPatch(listOf("update", "me")) }

    private val scripts: Map<String, String> by option().associate()

    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    val img by option()
        .file(canBeDir = false, mustBeReadable = true)
        .defaultLazy { (shared[Path::class] as Path).toFile() }

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache

        with(img.toPath()) {
            TermUi.debug(os)
            TermUi.debug(size)
            TermUi.debug(enableSsh)
            TermUi.debug(usernameRename)
            TermUi.debug(passwordChange)
            TermUi.debug(usbOtgProfiles)

//            patch.patch(img)
//            patcher(this, listOfNotNull(enableSsh, usernameRename, passwordChange, usbOtgProfiles))

            exitProcess(0)

            val logger = BlockRenderingLogger<Any>("Project: $name")

            val guestfish = Guestfish(this, logger, "$name-guestfish")
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
