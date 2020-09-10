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
import com.github.ajalt.clikt.parameters.options.transformValues
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.INSTANCE
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.cli.Config4kValueSource
import com.imgcstmzr.cli.Config4kValueSource.Companion.update
import com.imgcstmzr.cli.Env
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.SshPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.process.Downloader
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.OS
import com.imgcstmzr.runtime.SupportedOS
import com.imgcstmzr.runtime.Workflow
import com.imgcstmzr.runtime.WorkflowRuntime
import com.imgcstmzr.util.Paths
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.system.exitProcess

fun main() {
//    CliCommand().subcommands(ImgCommand(), CstmzrCommand(), FlshCommand()).main(listOf("--help"))
    CliCommand().subcommands(ImgCommand(), CstmzrCommand(), FlshCommand()).main(
        listOf(
            "--config", "bother-you.conf", "img",
            "--reuse-last-working-copy", "cstmzr"
        )
    )
//    CliCommand().subcommands(XyzCommands(), ImgCommand(), FlshCommand()).main(listOf("img", "--name", "name-by-cmdline"))
//    CliCommand().subcommands(XyzCommands(), ImgCommand(), FlshCommand()).main(listOf("img"))
//    F1().subcommands(DriverCommands(), CircuitCommands(), FlshCommand()).main(args)
}


@OptIn(ExperimentalValueSourceApi::class)
class CliCommand : NoOpCliktCommand(
    epilog = "((ε(*･ω･)_/${INSTANCE.colorize("ﾟ･.*･･｡☆")}",
    name = "imgcstmzr",
    allowMultipleSubcommands = true,
    printHelpOnEmptyArgs = true,
    help = "Downloads and Customizes Raspberry Pi Images"
) {
    init {
        context {
            helpFormatter = INSTANCE
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
            echo((tc.cyan + tc.bold)("Using config (file: $it, name: $name, size: ${it.readText().length})"))
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
    private val os by option().enum<SupportedOS>().default(SupportedOS.RPI_LITE)
    private val reuseLastWorkingCopy: Boolean by option().flag(default = false)
    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache

        shared[Path::class] = cache.provideCopy(name, reuseLastWorkingCopy) {
            val downloadUrl: String = os.downloadUrl ?: throw NoSuchElementException("$os has no download URL.")
            Downloader.download(downloadUrl).also { TermUi.echo("Downloaded $it from $downloadUrl") }
        }
    }
}

class CstmzrCommand : CliktCommand(help = "Customizes the given IMG") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "customize" to listOf("cstmzr"),
        "cstmz" to listOf("cstmzr"),
    )

    private val name by option().prompt(default = "my-img")

    private val os by option().enum<SupportedOS>().default(SupportedOS.RPI_LITE)
    private val size by option().long()
    private val enableSsh by option().convert { SshPatch() }
    private val renameUsername by option().transformValues(2) { list: List<String> -> UsernamePatch(list[0], list[1]) }
    private val changePassword by option().transformValues(2) { list: List<String> -> PasswordPatch(list[0], list[1]) }
    private val usbOtg by option().convert { UsbOnTheGoPatch() }

    private val scripts: Map<String, String> by option().associate()

    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    val img by option()
        .file(canBeDir = false, mustBeReadable = true)
        .defaultLazy { (shared[Path::class] as Path).toFile() }

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache
        val os: OS<Workflow> = os.invoke()

        with(img.toPath()) {

            val guestfish = Guestfish(this, "$name-guestfish")
            guestfish.run(copyOutCommands(listOf("/etc/hostname", "/boot/cmdline.txt", "/boot/config.txt").map(Path::of)))
            guestfish.guestRootOnHost
                .also { echo("Success $it") }

            exitProcess(0)
            val runtime = WorkflowRuntime(name)
            size?.let { os.increaseDiskSpace(size!!, this, runtime) }
//            runtime.bootAndRun("test", this, os.login("pi", "raspberry"), os.sequence("test", "wget https://bkahlert.com/wp-content/uploads/2019/08/Retrospective-02-Flipchart-Featured-720x405.jpg"))
            scripts.forEach { (scriptName, script) ->
                runtime.bootAndRun(scriptName, os, this, *os.compileSetupScript(scriptName, script))
            }
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
