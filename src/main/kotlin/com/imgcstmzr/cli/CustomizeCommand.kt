package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemImage.Companion.based
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.patch.patch
import com.imgcstmzr.util.Downloader
import koodies.docker.Docker
import koodies.exception.toCompactString
import koodies.io.path.getSize
import koodies.kaomoji.Kaomoji
import koodies.text.Banner
import koodies.tracing.rendering.ReturnValues
import koodies.tracing.rendering.Styles
import koodies.tracing.spanning
import koodies.unit.BinaryPrefixes
import java.nio.file.Path

class CustomizeCommand : NoOpCliktCommand(
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

    private val configFile: Path by option("--config", "--config-file", help = "Configuration to be used for image customization.")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true).required()

    private val envFile: Path by option("--env", "--env-file", help = ".env file that can be used to pass credentials like a new user password.")
        .path(mustExist = false, canBeDir = false, mustBeReadable = false).default(ImgCstmzr.HomeDirectory.resolve(".env.imgcstmzr"))

    private val cacheDir: Path by option("--cache-dir", help = "Directory that stores images, downloads, etc.")
        .path(canBeFile = false, mustBeWritable = true)
        .default(ImgCstmzr.Cache)

    private val reuseLastWorkingCopy: Boolean by option(help = "Whether to re-use the last working copy (instead of creating a new one).")
        .flag(default = false)

    private val cache: Cache by lazy { Cache(cacheDir) }

    override fun run() {
        echo(Banner.banner("ImgCstmzr"))
        echo()

        val config: CustomizationConfig = spanning(
            "Configuring",
            nameFormatter = PATCH_NAME_FORMATTER,
            decorationFormatter = PATCH_DECORATION_FORMATTER,
            layout = Layouts.DESCRIPTION,
            style = Styles.Dotted,
        ) {
            configFile.let {
                log("Configuration: ${it.toUri()} (${it.getSize().toString(BinaryPrefixes)})")
                CustomizationConfig.load(it, envFile)
            }.apply {
                log("Name: $name")
                log("OS: $os")
                log("Env: ${envFile.toUri()}")
                log("Cache: ${cacheDir.toUri()}")
            }
        }

        val osImage: OperatingSystemImage = spanning(
            "Preparing",
            nameFormatter = PATCH_NAME_FORMATTER,
            decorationFormatter = PATCH_DECORATION_FORMATTER,
            layout = Layouts.DESCRIPTION,
            style = Styles.Dotted,
        ) {
            require(Docker.engineRunning) { "Docker is required to be running but could not be found." }
            listOf(LibguestfsImage, DockerPiImage).subtract(Docker.images.list()).forEach { it.pull() }
            provideImageCopy(config)
        }

        val patches = config.toOptimizedPatches()
        val exceptions: ReturnValues<Throwable> = osImage.patch(*patches.toTypedArray())

        echo()

        if (exceptions.isEmpty()) {
            echo("${Kaomoji.Magical.random()} ${osImage.file.fileName} @ ${osImage.directory.toUri()}")
        } else {
            echo(Kaomoji.BadMood.random().also {
                echo("The following problems occurred during image customization:")
                exceptions.forEach { ex ->
                    echo(ex.toCompactString())
                }
            })
        }
    }

    private fun provideImageCopy(config: CustomizationConfig) = with(cache) {
        config.os based provideCopy(config.name, reuseLastWorkingCopy) {
            downloader.download(config.os.downloadUrl)
        }
    }
}
