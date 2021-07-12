package com.imgcstmzr.test

import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.ImgCstmzrTest
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.libguestfs.ImageBuilder
import com.imgcstmzr.libguestfs.ImageBuilder.buildFrom
import com.imgcstmzr.os.OperatingSystem
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemImage.Companion.based
import com.imgcstmzr.os.OperatingSystems
import com.imgcstmzr.util.Downloader
import koodies.collections.addElement
import koodies.io.path.deleteOnExit
import koodies.io.selfCleaning
import koodies.text.Semantics.formattedAs
import koodies.time.hours
import koodies.tracing.rendering.BlockStyles.Dotted
import koodies.tracing.spanning
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
        spanning(
            "Provisioning ${operatingSystem.fullName.formattedAs.input}",
            blockStyle = Dotted,
        ) {
            operatingSystem.getCopy(extensionContext.uniqueId).apply {
                if (autoDelete) file.deleteOnExit()
            }
        }
    }

    companion object {

        private val temp by ImgCstmzr.Temp.selfCleaning("download", 1.hours, 5)

        private val lock: ReentrantLock = ReentrantLock()

        private val downloader = Downloader(temp, ImageBuilder.schema to ::buildFrom)

        private val copiesPerTest = mutableMapOf<String, List<Path>>()
        fun cacheDir(uniqueId: String): Path? = copiesPerTest[uniqueId]?.firstOrNull()

        private val cache = Cache(ImgCstmzrTest.TestCache)
        private fun OperatingSystem.getCopy(uniqueId: String): OperatingSystemImage =
            lock.withLock {
                this@getCopy based with(cache) {
                    provideCopy(name, reuseLastWorkingCopy = false) {
                        with(downloader) { download() }
                    }.also {
                        copiesPerTest.addElement(uniqueId, it)
                    }
                }
            }

        fun Path.prepareSharedDirectory(): Path = ImgClassPathFixture.copyTo(resolve("shared").createDirectories())
    }
}
