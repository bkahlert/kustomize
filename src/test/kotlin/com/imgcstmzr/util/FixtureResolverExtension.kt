package com.imgcstmzr.util

import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.process.Downloader.download
import com.imgcstmzr.process.ImageBuilder
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.OperatingSystemMock
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
import kotlin.reflect.KClass

@Retention(RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class OS(
    val value: KClass<out OperatingSystem> = OperatingSystemMock::class,
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
        val operatingSystem: OperatingSystem = annotation?.value?.objectInstance ?: OperatingSystemMock()
        val autoDelete = annotation?.autoDelete ?: true
        val logger: RenderingLogger<OperatingSystemImage> = if (!extensionContext.isCapturingOutput()) {
            BlockRenderingLogger("Provisioning an image containing ${operatingSystem.name.cyan()} (${operatingSystem.directoryName})...")
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
        val OperatingSystem.directoryName get() = downloadUrl?.let { Path.of(it).baseName } ?: "imgcstmzr"

        private val cache = Cache(Paths.TEST.resolve("test"), maxConcurrentWorkingDirectories = 500)
        private fun OperatingSystem.getCopy(logger: RenderingLogger<*>): OperatingSystemImage = synchronized(this) {
            this based cache.provideCopy(directoryName, false, logger) {
                if (downloadUrl != null) download() else prepareImg(logger, "cmdline.txt", "config.txt")
            }
        }

        /**
         * Dynamically creates a raw image with two partitions containing the given [classPathFiles].
         */
        private fun prepareImg(logger: RenderingLogger<*>, vararg classPathFiles: String): Path =
            ImageBuilder.buildFrom(Paths.TEMP.resolve("imgcstmzr-" + String.random(4)).mkdirs().run {
                classPathFiles.forEach { classPathFile -> ClassPath.of(classPathFile).copyTo(resolve(classPathFile)) }
                while (size < 4.Mebi.bytes) resolve("fill.txt").appendText(ClassPath("Journey to the West - Introduction.txt").readAll())
                tarGzip()
            }, logger, freeSpaceRatio = 0.20)

        fun prepareSharedDirectory(): Path {
            val root = createTempDir("imgcstmzr")
            val boot = root.resolve("boot").toPath()
            ClassPath.of("cmdline.txt").copyToDirectory(boot)
            ClassPath.of("config.txt").copyToDirectory(boot)
            return root.toPath()
        }
    }
}
