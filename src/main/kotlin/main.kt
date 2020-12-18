import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.nio.file.Paths.HomeDirectory
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.colorize
import com.bkahlert.koodies.unit.Size.Companion.size
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.imgcstmzr.ImgCstmzrConfig
import com.imgcstmzr.cli.Banner.banner
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.copyOut
import com.imgcstmzr.patch.patch
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.logging
import com.imgcstmzr.tools.Downloader
import java.nio.file.Path

val debug = true

fun main() {
//    CliCommand().subcommands(ImgCommand(), CstmzrCommand(), FlshCommand()).main(listOf("--help"))
    CliCommand().main(
        listOf(
            "--config", "bother-you.conf"//, "--reuse-last-working-copy",
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

    private val downloader: Downloader = Downloader()

    init {
        context {
            autoEnvvarPrefix = "IMGCSTMZR"
            helpFormatter = ColorHelpFormatter()
        }
    }

    private val configFile: Path by option("--config", "--config-file", help = "configuration to be used for image customization")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true).required()

    private val envFile: Path by option("--env", "--env-file", help = ".env file that can be used to pass credentials like a new user password")
        .path(mustExist = false, canBeDir = false, mustBeReadable = false).default(HomeDirectory.resolve(".env.imgcstmzr"))

    private val cacheDir: Path by option("--cache-dir", help = "temporary directory that holds intermediary states to improve performance")
        .path(canBeFile = false, mustBeWritable = true)
        .default(Cache.DEFAULT)

    private val reuseLastWorkingCopy: Boolean by option().flag(default = false)

    private val flashImage: Boolean by option("--flash", help = "If an SD card is present, it will be flashed with the newly customized image.")
        .flag()

    private lateinit var cache: Cache

    override fun run() {
        cache = Cache(cacheDir)

        echo(banner("ImgCstmzr"))

        lateinit var config: ImgCstmzrConfig
        lateinit var osImage: OperatingSystemImage
        logging(banner("Configuration")) {
            config = configFile.let {
                logLine { "File: ${it.serialized}".cyan().bold() }
                logLine { "Size: ${it.size}".cyan().bold() }
                ImgCstmzrConfig.load(it, envFile)
            }.apply {
                logLine { "Name: $name".cyan().bold() }
                logLine { "Env: $envFile".cyan().bold() }
                logLine { "Cache: $cacheDir".cyan().bold() }

                osImage = provideImageCopy(this)
                logLine { "OS: $osImage" }
            }
        }

        val patches = config.toOptimizedPatches()
        val exceptions: List<Throwable> = patches.patch(osImage)

        logging(banner("Summary")) {
            logLine { "Run Scripts: " + osImage.copyOut("/usr/lib/virt-sysprep").toUri() }
            logLine { "Run Scripts Log: " + osImage.copyOut("/root/virt-sysprep-firstboot.log").toUri() }

            if (exceptions.isEmpty()) Kaomojis.Magic.random().toString() + " ${osImage.file.toUri()}"
            else Kaomojis.BadMood.random().also {
                logLine { "The following problems occurred during image customization:" }
                exceptions.forEach { ex ->
                    this.logCaughtException { ex }
                }
            }
        }
    }

    private fun BlockRenderingLogger.provideImageCopy(config: ImgCstmzrConfig) = with(cache) {
        config.os based provideCopy(config.name, reuseLastWorkingCopy) {
            with(downloader) {
                config.os.download(this@provideImageCopy).also {
                    logLine { "Download completed." }
                }
            }
        }
    }
}
