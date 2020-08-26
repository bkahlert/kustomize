import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.INSTANCE
import com.imgcstmzr.cli.Config4kValueSource
import com.imgcstmzr.cli.Config4kValueSource.Companion.update
import com.imgcstmzr.process.Downloader
import com.imgcstmzr.runtime.ArmRuntime
import com.imgcstmzr.runtime.WellKnownOperatingSystems
import java.io.File
import kotlin.reflect.KClass

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
            helpFormatter = ColorHelpFormatter()
            valueSource = Config4kValueSource.proxy()
        }
    }

    private val configFile: File? by option("--config", "--config-file", help = "configuration to be used for image customization")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val name: String? by option(help = "name of the generated appliance").default("my-img")

    private val cacheDir: File by option("--cache-dir", help = "temporary directory that holds intermediary states to improve performance")
        .file(canBeFile = false, mustBeWritable = true)
        .default(Cache.DEFAULT)

    private val shared by findOrSetObject { mutableMapOf<KClass<*>, Any>() }

    override fun run() {
        configFile?.also {
            currentContext.valueSource.update(it)
            echo("Using config (file: $it, name: $name, size: ${it.readText().length})")
        }
        shared[Cache::class] = Cache(cacheDir)
        echo("Using cache (path: $cacheDir)")
    }
}

class ImgCommand : CliktCommand(help = "Provides an IMG file containing the specified OS") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "image" to listOf("img"),
    )

    private val name by option().default("my-img")
    private val os: WellKnownOperatingSystems by option().enum<WellKnownOperatingSystems>().default(WellKnownOperatingSystems.RASPBERRY_PI_OS_LITE)
    private val reuseLastWorkingCopy by option().flag(default = false)
    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache

        shared[File::class] = cache.provideCopy(name, reuseLastWorkingCopy) {
            Downloader().download(os.downloadUrl).also { TermUi.echo("Downloaded $it from ${os.downloadUrl}") }
        }
    }
}

class CstmzrCommand : CliktCommand(help = "Customizes the given IMG") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "customize" to listOf("cstmzr"),
        "cstmz" to listOf("cstmzr"),
    )

    private val name by option().prompt(default = "my-img")
    private val os by option().enum<WellKnownOperatingSystems>().default(WellKnownOperatingSystems.RASPBERRY_PI_OS_LITE)
    private val size by option().long()
    private val commands: Map<String, String> by option().associate()

    private val shared by requireObject<MutableMap<KClass<*>, Any>>()

    val img by option()
        .file(canBeDir = false, mustBeReadable = true)
        .defaultLazy { shared[File::class] as File }

    override fun run() {
        val system = ArmRuntime(os, name, img)
        size?.let { system.increaseDiskSpace(size!!) }
        commands.forEach {
            system.bootAndRun(it.key, os.login("pi", "raspberry"), *os.sequences(it.key, it.value))
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
