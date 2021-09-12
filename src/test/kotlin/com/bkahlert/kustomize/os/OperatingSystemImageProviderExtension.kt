package com.bkahlert.kustomize.os

import com.bkahlert.kommons.collections.addElement
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.executionResult
import com.bkahlert.kommons.test.get
import com.bkahlert.kommons.test.put
import com.bkahlert.kommons.test.storeForNamespaceAndTest
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.tracing.rendering.Styles.Dotted
import com.bkahlert.kommons.tracing.rendering.runSpanningLine
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kustomize.TestKustomize.testCacheDirectory
import com.bkahlert.kustomize.cli.Cache
import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.os.OperatingSystemImage.Companion.based
import com.bkahlert.kustomize.test.ImageFixtures
import com.bkahlert.kustomize.util.Downloader
import org.junit.jupiter.api.extension.AfterEachCallback
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
)

open class OperatingSystemImageProviderExtension : TypeBasedParameterResolver<OperatingSystemImage>(), AfterEachCallback {

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): OperatingSystemImage = lock.withLock {
        val annotation = parameterContext.parameter.getAnnotation(OS::class.java)
        val operatingSystem: OperatingSystem = annotation?.value ?: OperatingSystems.RiscTestOS
        runSpanning(
            "Provisioning ${operatingSystem.fullName.formattedAs.input}",
            style = Dotted,
            layout = Layouts.DESCRIPTION,
        ) {
            operatingSystem.getCopy(extensionContext.uniqueId).also {
                extensionContext.store().put(it.directory)
            }
        }
    }

    override fun afterEach(context: ExtensionContext) {
        context.store().get<Path>()?.also { dir ->
            runSpanningLine(
                "Cleaning up",
                style = Dotted,
                layout = Layouts.DESCRIPTION,
            ) {
                if (context.executionResult.isSuccess) {
                    log("Test succeeded".formattedAs.success)
                    log("Deleting ${dir.pathString.ansi.strikethrough}")
                    kotlin.runCatching { dir.deleteRecursively() }
                        .onFailure {
                            log("Error cleaning up")
                            ShellScript("ls -lisaR .").exec.logging(dir)
                            exception(it)
                        }
                } else {
                    log("Test failed".formattedAs.error)
                    log("Keeping ${dir.pathString.ansi.bold}")
                }
            }
        }
    }

    companion object {
        private val store by storeForNamespaceAndTest()

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
