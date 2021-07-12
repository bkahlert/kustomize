import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.ImgCstmzrConfig
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemImage.Companion.based
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.patch.patch
import com.imgcstmzr.util.Disk
import com.imgcstmzr.util.Downloader
import com.imgcstmzr.util.flash
import koodies.collections.plus
import koodies.docker.Docker
import koodies.exception.toCompactString
import koodies.io.path.getSize
import koodies.io.path.pathString
import koodies.kaomoji.Kaomoji
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.colorize
import koodies.text.Banner.banner
import koodies.text.Semantics.formattedAs
import koodies.tracing.rendering.ReturnValues
import koodies.tracing.spanning
import java.nio.file.Path

val debug = true
val reuseLastWorkingCopyByDefault = false

fun main(vararg args: String) = CliCommand().main(args.let { if (reuseLastWorkingCopyByDefault) it + "--reuse-last-working-copy" else it })

class CliCommand : NoOpCliktCommand(
    epilog = "((ε(*･ω･)_/${"ﾟ･.*･･｡☆".ansi.colorize()}",
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
        .path(mustExist = false, canBeDir = false, mustBeReadable = false).default(ImgCstmzr.HomeDirectory.resolve(".env.imgcstmzr"))

    private val cacheDir: Path by option("--cache-dir", help = "temporary directory that holds intermediary states to improve performance")
        .path(canBeFile = false, mustBeWritable = true)
        .default(ImgCstmzr.Cache)

    private val reuseLastWorkingCopy: Boolean by option().flag(default = false)

    private val flashImage: Boolean by option("--flash", help = "If an SD card is present, it will be flashed with the newly customized image.")
        .flag()

    private val cache: Cache by lazy { Cache(cacheDir) }

    override fun run() {
        echo(banner("ImgCstmzr"))
        echo()

        lateinit var config: ImgCstmzrConfig
        lateinit var osImage: OperatingSystemImage
        spanning(banner("Configuration")) {
            config = configFile.let {
                log("File: ${it.pathString}".ansi.cyan.bold)
                log("Size: ${it.getSize()}".ansi.cyan.bold)
                ImgCstmzrConfig.load(it, envFile)
            }.apply {
                log("Name: $name".ansi.cyan.bold)
                log("Env: $envFile".ansi.cyan.bold)
                log("Cache: $cacheDir".ansi.cyan.bold)

                osImage = provideImageCopy(this)
                log("OS: $osImage".ansi.cyan.bold)
            }
        }

        require(Docker.engineRunning) { "Docker is required to be running but could not be found." }
        // TODO get complete id for Docker.images
        listOf(LibguestfsImage, DockerPiImage).subtract(Docker.images.list()).forEach { it.pull() }

        val patches = config.toOptimizedPatches()
        val exceptions: ReturnValues<Throwable> = patches.patch(osImage)

        val flashDisk: Disk? = spanning(banner("Flashing $osImage")) {
            config.flashDisk?.let { disk ->
                flash(osImage.file, disk.takeUnless { it.equals("auto", ignoreCase = true) })
            } ?: run {
                log(
                    """
                        
                    To make use of your image, you have the following options:
                    a) Check you actually inserted an SD card. 
                       Also an once ejected SD card needs to be re-inserted to get recognized.
                    b) Set ${ImgCstmzrConfig::flashDisk.name.formattedAs.input} to ${"auto".formattedAs.input} for flashing to any available physical removable disk.
                    c) Set ${ImgCstmzrConfig::flashDisk.name.formattedAs.input} explicitly to any available physical removable disk (e.g. ${"disk2".formattedAs.input}) if auto fails.
                    d) ${"Flash manually".formattedAs.input} using the tool of your choice.
                       ${osImage.file.toUri()}
                """.trimIndent()
                )
                null
            }
        }

        echo()

        spanning(banner("Summary")) {
            log("Image flashed to: " + (flashDisk?.run { toString() } ?: "—"))
            log("Run scripts: " + osImage.copyOut("/usr/lib/virt-sysprep").toUri())
            log("Run scripts log: " + osImage.copyOut("/root/virt-sysprep-firstboot.log").toUri())

            if (exceptions.isEmpty()) Kaomoji.Magical.random().toString() + " ${osImage.file.toUri()}"
            else Kaomoji.BadMood.random().also {
                log("The following problems occurred during image customization:")
                exceptions.forEach { ex ->
                    log(ex.toCompactString())
                }
            }
        }
    }

    private fun provideImageCopy(config: ImgCstmzrConfig) = with(cache) {
        config.os based provideCopy(config.name, reuseLastWorkingCopy) {
            with(downloader) {
                config.os.download()
            }
        }
    }
}
