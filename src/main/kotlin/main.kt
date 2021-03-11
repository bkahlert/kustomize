import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.imgcstmzr.ImgCstmzrConfig
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.copyOut
import com.imgcstmzr.patch.patch
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.OperatingSystemProcess
import com.imgcstmzr.util.Disk
import com.imgcstmzr.util.Downloader
import com.imgcstmzr.util.flash
import koodies.collections.plus
import koodies.concurrent.output
import koodies.concurrent.script
import koodies.docker.Docker
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.io.path.getSize
import koodies.kaomoji.Kaomojis
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.terminal.ANSI
import koodies.terminal.AnsiColors.cyan
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.Banner.banner
import koodies.terminal.colorize
import java.nio.file.Path

val debug = true
val reuseLastWorkingCopyByDefault = false

fun main(vararg args: String) = CliCommand().main(args.let { if (reuseLastWorkingCopyByDefault) it + "--reuse-last-working-copy" else it })

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
        .path(mustExist = false, canBeDir = false, mustBeReadable = false).default(Locations.HomeDirectory.resolve(".env.imgcstmzr"))

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
        echo()

        lateinit var config: ImgCstmzrConfig
        lateinit var osImage: OperatingSystemImage
        logging(banner("Configuration")) {
            config = configFile.let {
                logLine { "File: ${it.asString()}".cyan().bold() }
                logLine { "Size: ${it.getSize()}".cyan().bold() }
                ImgCstmzrConfig.load(it, envFile)
            }.apply {
                logLine { "Name: $name".cyan().bold() }
                logLine { "Env: $envFile".cyan().bold() }
                logLine { "Cache: $cacheDir".cyan().bold() }

                osImage = provideImageCopy(this)
                logLine { "OS: $osImage".cyan().bold() }
            }
        }

        require(Docker.engineRunning) { "Docker is required to be running but could not be found." }
        listOf(LibguestfsCommandLine.DOCKER_IMAGE, OperatingSystemProcess.DOCKER_IMAGE).subtract(Docker.images).forEach {
            logging("Downloading required Docker image $it") {
                script(logger = this) { !"docker pull $it" }.output()
            }
        }

        echo()
        val patches = config.toOptimizedPatches()
        val exceptions: List<Throwable> = patches.patch(osImage)

        val flashDisk: Disk? = logging("Flashing $osImage") {
            config.flashDisk?.let { disk ->
                flash(osImage.file, disk.takeUnless { it.equals("auto", ignoreCase = true) })
            } ?: run {
                logLine {
                    """
                        
                    To make use of your image, you have the following options:
                    a) Check you actually inserted an SD card. 
                       Also an once ejected SD card needs to be re-inserted to get recognized.
                    b) Set ${ImgCstmzrConfig::flashDisk.name.cyan()} to ${"auto".cyan()} for flashing to any available physical removable disk.
                    c) Set ${ImgCstmzrConfig::flashDisk.name.cyan()} explicitly to any available physical removable disk (e.g. ${"disk2".cyan()}) if auto fails.
                    d) ${"Flash manually".cyan()} using the tool of your choice.
                       ${osImage.file.toUri()}
                """.trimIndent()
                }
                null
            }
        }

        echo()

        logging(banner("Summary")) {
            logLine { "Image flashed to: " + (flashDisk?.run { toString() } ?: "—") }
            logLine { "Run scripts: " + osImage.copyOut("/usr/lib/virt-sysprep").toUri() }
            logLine { "Run scripts log: " + osImage.copyOut("/root/virt-sysprep-firstboot.log").toUri() }

            if (exceptions.isEmpty()) Kaomojis.Magic.random().toString() + " ${osImage.file.toUri()}"
            else Kaomojis.BadMood.random().also {
                logLine { "The following problems occurred during image customization:" }
                exceptions.forEach { ex ->
                    this.logCaughtException { ex }
                }
            }
        }
    }

    private fun RenderingLogger.provideImageCopy(config: ImgCstmzrConfig) = with(cache) {
        config.os based provideCopy(config.name, reuseLastWorkingCopy) {
            with(downloader) {
                config.os.download(this@provideImageCopy).also {
                    logLine { "Download completed." }
                }
            }
        }
    }
}
