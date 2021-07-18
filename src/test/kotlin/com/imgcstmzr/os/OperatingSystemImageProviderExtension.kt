package com.imgcstmzr.os

import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.TestImgCstmzr
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.cli.Layouts
import com.imgcstmzr.libguestfs.ImageBuilder
import com.imgcstmzr.libguestfs.ImageBuilder.buildFrom
import com.imgcstmzr.os.OperatingSystemImage.Companion.based
import com.imgcstmzr.test.ImageFixtures
import com.imgcstmzr.util.Downloader
import koodies.collections.addElement
import koodies.io.path.deleteOnExit
import koodies.io.selfCleaning
import koodies.text.Semantics.formattedAs
import koodies.time.hours
import koodies.tracing.rendering.Styles.Dotted
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

open class OperatingSystemImageProviderExtension : TypeBasedParameterResolver<OperatingSystemImage>() {

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): OperatingSystemImage = lock.withLock {
        val annotation = parameterContext.parameter.getAnnotation(OS::class.java)
        val operatingSystem: OperatingSystem = annotation?.value ?: OperatingSystems.ImgCstmzrTestOS
        val autoDelete = annotation?.autoDelete ?: true
        spanning(
            "Provisioning ${operatingSystem.fullName.formattedAs.input}",
            style = Dotted,
            layout = Layouts.DESCRIPTION,
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

        private val cache = Cache(TestImgCstmzr.TestCache)
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

        fun Path.prepareSharedDirectory(): Path = ImageFixtures.copyTo(resolve("shared").createDirectories())
    }
}
