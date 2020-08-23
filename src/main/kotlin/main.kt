import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.mordant.TermColors
import com.imgcstmzr.ArmRuntime
import com.imgcstmzr.Cache
import com.imgcstmzr.ConfigValueSource
import com.imgcstmzr.DelegatingValueSource
import com.imgcstmzr.Downloader
import java.io.File
import java.time.LocalDate
import kotlin.reflect.KClass

class UserOptions : OptionGroup(
    name = "User Options",
    help = "Options controlling the user"
) {
    val name by option(help = "user name")
    val age by option(help = "user age").int()
}

class ColorHelpFormatter : CliktHelpFormatter(showDefaultValues = true) {
    private val tc = TermColors(TermColors.Level.ANSI256)

    override fun renderTag(tag: String, value: String) = tc.green(super.renderTag(tag, value))
    override fun renderOptionName(name: String) = tc.yellow(super.renderOptionName(name))
    override fun renderArgumentName(name: String) = tc.yellow(super.renderArgumentName(name))
    override fun renderSubcommandName(name: String) = tc.yellow(super.renderSubcommandName(name))
    override fun renderSectionTitle(title: String) = (tc.bold + tc.underline)(super.renderSectionTitle(title))
    override fun optionMetavar(option: HelpFormatter.ParameterHelp.Option) = tc.green(super.optionMetavar(option))
}

@OptIn(ExperimentalValueSourceApi::class)
class CliCommand :
    NoOpCliktCommand(
        name = "imgcstmzr",
        allowMultipleSubcommands = true,
        printHelpOnEmptyArgs = true,
        help = "Downloads and Customizes Raspberry Pi Images"
    ) {
    val valueSourceDelegator = DelegatingValueSource<ConfigValueSource>()

    init {
        context {
            helpFormatter = ColorHelpFormatter()
            valueSource = valueSourceDelegator
        }
    }

    val config: String? by option("--config", "--config-file", help = "(partial) configuration to be used for image customization")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert {
            val value = it.readText()
            valueSourceDelegator.delegate = ConfigValueSource.parse(value)
            value
        }
    val name: String? by option().prompt(default = "my-img")

    val cache: Cache by option("--cache-dir", help = "temporary directory that holds intermediary states to improve performance")
        .file(canBeFile = false, mustBeWritable = true)
        .convert { Cache(it) }
        .default(Cache())

    val shared by findOrSetObject { mutableMapOf<KClass<*>, Any>() }

    override fun run() {
        shared[Cache::class] = cache
        config?.also { echo("Using config (name: $name, size: ${it.length})") }
        echo("Using cache (path: ${cache.dir})")
    }
}

class ImgCommand : CliktCommand() {
    val name by option().prompt(default = "my-img")
    val url by option().default("downloads.raspberrypi.org/raspios_lite_armhf_latest")
    val reuseLastWorkingCopy by option().flag(default = false)
    val shared by requireObject<MutableMap<KClass<*>, Any>>()

    override fun run() {
        val cache: Cache = shared[Cache::class] as Cache

        shared[File::class] = cache.provideCopy(name, reuseLastWorkingCopy) {
            Downloader().download(url).also { TermUi.echo("Downloaded $url to $it") }
        }
    }
}

class CstmzCommand : CliktCommand() {
    val name by option().prompt(default = "my-img")
    val shared by requireObject<MutableMap<KClass<*>, Any>>()

    val img by option()
        .file(canBeDir = false, mustBeReadable = true)
        .defaultLazy { shared[File::class] as File }

    override fun run() {
        val system = ArmRuntime(name, img)
        echo(system)
    }
}

@OptIn(ExperimentalValueSourceApi::class)
class XyzCommands : CliktCommand(name = "xyz") {



    init {
        context {
            valueSource = PropertiesValueSource.from("myconfig.properties")
        }
    }

    val cacheDir: Cache by option("--cache-dir", help = "temporary directory that holds intermediary states to improve performance")
        .file(canBeFile = false, mustBeWritable = true)
        .convert { Cache(it) }
        .default(Cache())

    val password by option().prompt(requireConfirmation = true, hideInput = true)

//    val file: String by argument()
//        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
//        .convert { it.copyTo() }
//        .convert { it.readText() }

    override fun run() {
        echo("Xyz.run()")
        echo(cacheDir)
        echo("Drivers Yeah Yeah")
        echo("Your hidden password: $password")
    }

}

class CircuitCommands : CliktCommand(name = "circuits") {
    val season: Int by option(help = "Season year").int().default(LocalDate.now().year)

    override fun run() {
    }
}


fun main() {
    CliCommand().subcommands(ImgCommand(), CstmzCommand()).main(
        listOf(
            "--config", "bother-you.conf", "img",
            "--reuse-last-working-copy", "cstmz"
        )
    )
//    CliCommand().subcommands(XyzCommands(), ImgCommand()).main(listOf("img", "--name", "name-by-cmdline"))
//    CliCommand().subcommands(XyzCommands(), ImgCommand()).main(listOf("img"))
//    F1().subcommands(DriverCommands(), CircuitCommands()).main(args)
}
