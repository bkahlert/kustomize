package com.bkahlert.kustomize.cli

import com.bkahlert.kustomize.KustomizeTelemetry
import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystemImage.Companion.based
import com.bkahlert.kustomize.os.OperatingSystemProcess.Companion.DockerPiImage
import com.bkahlert.kustomize.patch.patch
import com.bkahlert.kustomize.util.Downloader
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import koodies.docker.Docker
import koodies.exception.toCompactString
import koodies.io.path.asPath
import koodies.io.path.getSize
import koodies.kaomoji.Kaomoji
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Banner
import koodies.text.Semantics.formattedAs
import koodies.tracing.rendering.ReturnValues
import koodies.tracing.rendering.Styles
import koodies.tracing.spanning
import koodies.unit.BinaryPrefixes
import java.nio.file.Path
import kotlin.io.path.isReadable

class CustomizeCommand : NoOpCliktCommand(
    name = "kustomize",
    allowMultipleSubcommands = true,
    printHelpOnEmptyArgs = true,
    help = "Downloads and Customizes Raspberry Pi Images"
) {

    private val downloader: Downloader = Downloader()

    init {
        context {
            autoEnvvarPrefix = "KUSTOMIZE"
            helpFormatter = ColorHelpFormatter()
        }
    }

    private val configFile: Path by option("--config-file", help = "Configuration to be used for image customization.")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true).required()

    private val envFile: Path by option("--env-file", help = ".env file to set environment variables")
        .path(mustExist = false, canBeDir = false, mustBeReadable = false).default(".env".asPath())

    private val cacheDir: Path by option("--cache-dir", help = "Directory in which downloaded and customized images are stored.")
        .path(mustExist = false, canBeFile = false)
        .default(".".asPath())

    private val jaegerTracing: Boolean by option(help = "Whether to trace the customization using a locally started Jaeger.")
        .flag(default = false)

    private val cache: Cache by lazy { Cache(cacheDir) }

    private val skipPatches: Boolean by option("--skip-patches", help = "Skips applying patches altogether.")
        .flag(default = false)

    override fun run() {
        if (jaegerTracing) {
            echo("Tracing UI: ${KustomizeTelemetry.tracerUI}".ansi.formattedAs.meta)
        }

        echo(Banner.banner("Kustomize"))
        echo()

        val config: CustomizationConfig = spanning(
            "Configuring",
            nameFormatter = PATCH_NAME_FORMATTER,
            decorationFormatter = PATCH_DECORATION_FORMATTER,
            layout = Layouts.DESCRIPTION,
            style = Styles.Dotted,
        ) {
            configFile.run {
                log("Configuration: ${toUri()} (${getSize().toString(BinaryPrefixes)})")
                CustomizationConfig.load(this, envFile.takeIf { it.isReadable() })
            }.apply {
                log("Name: $name")
                log("OS: $os")
                log("Env: ${envFile.toUri()}")
                log("Cache: ${cache.dir.toUri()}")
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

        val exceptions: ReturnValues<Throwable> =
            if (skipPatches) {
                ReturnValues()
            } else {
                val patches = config.toOptimizedPatches()
                osImage.patch(*patches.toTypedArray())
            }

        echo()

        if (exceptions.isEmpty()) {
            echo("${Kaomoji.Wizards.random()} ${osImage.file.fileName} @ ${osImage.directory.toUri()}")
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
        config.os based provideCopy(config.name) {
            downloader.download(config.os.downloadUrl)
        }
    }
}
