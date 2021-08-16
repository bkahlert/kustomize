package com.bkahlert.kustomize.os

import com.bkahlert.kommons.collections.addElement
import com.bkahlert.kommons.io.path.deleteOnExit
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.tracing.rendering.Styles.Dotted
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kustomize.TestKustomize.testCacheDirectory
import com.bkahlert.kustomize.cli.Cache
import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.os.OperatingSystemImage.Companion.based
import com.bkahlert.kustomize.test.ImageFixtures
import com.bkahlert.kustomize.util.Downloader
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
    val value: OperatingSystems = OperatingSystems.RiscTestOS,
    val autoDelete: Boolean = true,
)

open class OperatingSystemImageProviderExtension : TypeBasedParameterResolver<OperatingSystemImage>() {

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): OperatingSystemImage = lock.withLock {
        val annotation = parameterContext.parameter.getAnnotation(OS::class.java)
        val operatingSystem: OperatingSystem = annotation?.value ?: OperatingSystems.RiscTestOS
        val autoDelete = annotation?.autoDelete ?: true
        runSpanning(
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

        private val lock: ReentrantLock = ReentrantLock()

        private val downloader = Downloader()

        private val copiesPerTest = mutableMapOf<String, List<Path>>()
        fun cacheDir(uniqueId: String): Path? = copiesPerTest[uniqueId]?.firstOrNull()

        private val cache = Cache(testCacheDirectory)
        private fun OperatingSystem.getCopy(uniqueId: String): OperatingSystemImage =
            lock.withLock {
                this based with(cache) {
                    provideCopy(name) {
                        downloader.download(downloadUrl)
                    }.also {
                        copiesPerTest.addElement(uniqueId, it)
                    }
                }
            }

        fun Path.prepareSharedDirectory(): Path = ImageFixtures.copyTo(resolve("shared").createDirectories())
    }
}
