package com.imgcstmzr.util

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.guestfish.ImageBuilder
import com.imgcstmzr.process.Downloader
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.runLogging
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.OutputCaptureExtension.Companion.isCapturingOutput
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.nio.file.Path
import kotlin.annotation.AnnotationRetention.RUNTIME

@Retention(RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class OS(
    val value: OperatingSystems = OperatingSystems.ImgCstmzrTestOS,
    val autoDelete: Boolean = true,
)

open class FixtureResolverExtension : ParameterResolver {

    init {
        // provokes instantiation of FixtureLog to have it clean up first
        FixtureLog.location
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.isAssignableFrom(OperatingSystemImage::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): OperatingSystemImage = synchronized(this) {
        val annotation = parameterContext.parameter.getAnnotation(OS::class.java)
        val operatingSystem: OperatingSystem = annotation?.value ?: OperatingSystems.ImgCstmzrTestOS
        val autoDelete = annotation?.autoDelete ?: true
        val logger: RenderingLogger<OperatingSystemImage> = if (!extensionContext.isCapturingOutput()) {
            BlockRenderingLogger("Provisioning an image containing ${operatingSystem.fullName.cyan()} (${operatingSystem.name})...")
        } else {
            MutedBlockRenderingLogger()
        }
        logger.runLogging {
            operatingSystem.getCopy(logger).apply {
                if (autoDelete) deleteOnExit<Path>()
                logger.logText { "Provisioning " }
            }
        }
    }

    companion object {
        val downloader = Downloader(ImageBuilder.schema to { uri, logger -> ImageBuilder.buildFrom(uri, logger) })

        private val cache = Cache(Paths.TEST.resolve("test"), maxConcurrentWorkingDirectories = 500)
        private fun OperatingSystem.getCopy(logger: RenderingLogger<*>): OperatingSystemImage = synchronized(this) {
            this based cache.provideCopy(name, false, logger) {
                with(downloader) {
                    download(logger)
                }
            }
        }

        fun prepareSharedDirectory(): Path {
            val root = createTempDir("imgcstmzr")
            val boot = root.resolve("boot").toPath()
            ClassPath.of("cmdline.txt").copyToDirectory(boot)
            ClassPath.of("config.txt").copyToDirectory(boot)
            return root.toPath()
        }
    }
}
