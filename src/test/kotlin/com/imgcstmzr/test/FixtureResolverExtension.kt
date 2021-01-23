package com.imgcstmzr.test

import com.imgcstmzr.cli.Cache
import com.imgcstmzr.libguestfs.docker.ImageBuilder
import com.imgcstmzr.libguestfs.docker.ImageBuilder.buildFrom
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.test.logging.logger
import com.imgcstmzr.util.Downloader
import com.imgcstmzr.util.Paths
import koodies.collections.addElement
import koodies.io.path.Locations
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.runtime.deleteOnExit
import koodies.terminal.AnsiColors.cyan
import koodies.test.copyTo
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories

@Retention(RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class OS(
    val value: OperatingSystems = OperatingSystems.ImgCstmzrTestOS,
    val autoDelete: Boolean = true,
)

open class FixtureResolverExtension : TypeBasedParameterResolver<OperatingSystemImage>() {


    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): OperatingSystemImage = lock.withLock {
        val annotation = parameterContext.parameter.getAnnotation(OS::class.java)
        val operatingSystem: OperatingSystem = annotation?.value ?: OperatingSystems.ImgCstmzrTestOS
        val autoDelete = annotation?.autoDelete ?: true
        extensionContext.logger()
            .logging("Provisioning an image containing ${operatingSystem.fullName.cyan()} (${operatingSystem.name})...", bordered = false) {
                operatingSystem.getCopy(this, extensionContext.uniqueId).apply {
                    if (autoDelete) file.deleteOnExit()
                }
            }
    }

    companion object {
        private val lock: ReentrantLock = ReentrantLock()

        private val downloader = Downloader(
            Locations.Temp.resolve("imgcstmzr.downloads").createDirectories(),
            ImageBuilder.schema to { uri, logger -> logger.buildFrom(uri) })

        private val copiesPerTest = mutableMapOf<String, List<Path>>()
        fun cacheDir(uniqueId: String): Path? = copiesPerTest[uniqueId]?.firstOrNull()

        private val cache = Cache(Paths.TEST.resolve("test"))
        private fun OperatingSystem.getCopy(logger: RenderingLogger, uniqueId: String): OperatingSystemImage =
            lock.withLock {
                this@getCopy based with(cache) {
                    logger.provideCopy(name, reuseLastWorkingCopy = false) {
                        with(downloader) { download(logger) }
                    }.also {
                        copiesPerTest.addElement(uniqueId, it)
                    }
                }
            }

        fun Path.prepareSharedDirectory(): Path = ImgClassPathFixture.copyTo(resolve("shared").createDirectories())
    }
}
