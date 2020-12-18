package com.imgcstmzr.util

import com.bkahlert.koodies.concurrent.cleanUpOnShutDown
import com.bkahlert.koodies.nio.file.Paths.Temp
import com.bkahlert.koodies.nio.file.age
import com.bkahlert.koodies.nio.file.isEmpty
import com.bkahlert.koodies.nio.file.list
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.libguestfs.docker.ImageBuilder
import com.imgcstmzr.libguestfs.docker.ImageBuilder.buildFrom
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.MutedRenderingLogger
import com.imgcstmzr.runtime.log.runLogging
import com.imgcstmzr.tools.Downloader
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.OutputCaptureExtension.Companion.isCapturingOutput
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.nio.file.Path
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.time.days

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
        cleanUpOnShutDown { Temp.list().filter { it.isDirectory && it.isEmpty && it.age > 1.days } }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.isAssignableFrom(OperatingSystemImage::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): OperatingSystemImage = synchronized(this) {
        val annotation = parameterContext.parameter.getAnnotation(OS::class.java)
        val operatingSystem: OperatingSystem = annotation?.value ?: OperatingSystems.ImgCstmzrTestOS
        val autoDelete = annotation?.autoDelete ?: true
        val logger: BlockRenderingLogger = if (!extensionContext.isCapturingOutput()) {
            BlockRenderingLogger("Provisioning an image containing ${operatingSystem.fullName.cyan()} (${operatingSystem.name})...")
        } else {
            MutedRenderingLogger()
        }
        logger.runLogging {
            operatingSystem.getCopy(logger).apply {
                if (autoDelete) file.deleteOnExit()
                logger.logText { "Provisioning " }
            }
        }
    }

    companion object {
        private val downloader = Downloader(ImageBuilder.schema to { uri, logger -> logger.buildFrom(uri) })

        private val cache = Cache(Paths.TEST.resolve("test"), maxConcurrentWorkingDirectories = 500)
        private fun OperatingSystem.getCopy(logger: BlockRenderingLogger): OperatingSystemImage = synchronized(this) {
            this based with(cache) {
                logger.provideCopy(name, reuseLastWorkingCopy = false) {
                    with(downloader) { download(logger) }
                }
            }
        }

        fun prepareSharedDirectory(): Path = ImgFixture.copyTo(tempDir("imgcstmzr-").deleteOnExit())
    }
}
