package com.imgcstmzr.util

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.Cache
import com.imgcstmzr.process.Downloader.download
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemMock
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.nio.file.Path
import java.time.Instant.now
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.reflect.KClass

@Retention(RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class OS(val value: KClass<out OperatingSystem> = OperatingSystemMock::class, val autoDelete: Boolean = true)

open class FixtureExtension : ParameterResolver, AfterEachCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?): Boolean =
        Path::class.java.isAssignableFrom(parameterContext.parameter.type)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?): Any? {
        val annotation = parameterContext.parameter.getAnnotation(OS::class.java)
        val os = annotation?.value?.objectInstance
        val autoDelete = annotation?.autoDelete ?: true
        return cachedCopyOf(os).also { if (autoDelete) saveReferenceForCleanup(extensionContext ?: throw NoSuchElementException(), it) }
    }

    override fun afterEach(context: ExtensionContext) {
        cleanUp(context)
    }

    companion object {
        private val cache = Cache(Paths.TEST.resolve("test"), maxConcurrentWorkingDirectories = 20)
        private fun cachedCopyOf(os: OperatingSystem?): Path {
            val name = os?.downloadUrl?.let { Path.of(it).baseName } ?: "imgcstmzr"
            return cache.provideCopy(name, false) { os?.download() ?: prepareImg("cmdline.txt", "config.txt") }
        }

        /**
         * Dynamically creates a raw image with two partitions containing the given [files].
         */
        private fun prepareImg(vararg files: String): Path {
            val basename = "imgcstmzr"
            echo(META typed now().asEmoji() + " Preparing test img with files ${files.joinToString(", ")}. This takes a moment...")
            val hostDir = Paths.TEST.resolve(basename + String.random(4)).mkdirs()
            val copyFileCommands: List<String> = files.map {
                ClassPath.of(it).copyTo(hostDir.resolve(it))
                "copy-in ${Guestfish.DOCKER_MOUNT_ROOT.resolve(it)} /boot"
            }

            val imgName = "$basename.img"
            Guestfish.execute(
                containerName = imgName,
                volumes = mapOf(hostDir to Guestfish.DOCKER_MOUNT_ROOT),
                "sparse ${Guestfish.DOCKER_MOUNT_ROOT.resolve(imgName)} 4M", // = 8192 sectors
                "run",
                "part-init /dev/sda mbr",
                "echo \"num sectors:\"",
                "blockdev-getsz /dev/sda",
                "part-add /dev/sda p 2048 4095",
                "part-add /dev/sda p 4096 -2048",
                "mkfs vfat /dev/sda1",
                "mkfs ext4 /dev/sda2",
                "mount /dev/sda2 /",
                "mkdir /boot",
                "mount /dev/sda1 /boot",
                * copyFileCommands.toTypedArray(),
            )
            echo(META typed "Finished test img creation.")
            return hostDir.resolve(imgName)
        }

        fun prepareSharedDirectory(): Path {
            val root = createTempDir("imgcstmzr")
            val boot = root.resolve("boot").toPath()
            ClassPath.of("cmdline.txt").copyToDirectory(boot)
            ClassPath.of("config.txt").copyToDirectory(boot)
            return root.toPath()
        }

        private fun cleanUp(context: ExtensionContext) {
            deleteImgRelatedDirectories(context)
        }

        private fun deleteImgRelatedDirectories(context: ExtensionContext) {
            loadImg(context)
                ?.let {
                    val imgDirectory = it.parent
                    val imgDirectoryContainer = imgDirectory.parent
                    imgDirectoryContainer.listFilesRecursively({ other ->
                        other.toString().startsWith(imgDirectory.toString())
                    })
                }
                ?.onEach { imgDirectoryBasedCopy ->
                    imgDirectoryBasedCopy.delete(true)
                }
        }

        private fun loadImg(context: ExtensionContext): Path? =
            getStore(context).get(context.requiredTestInstance, Path::class.java)

        private fun saveReferenceForCleanup(context: ExtensionContext, img: Path) {
            getStore(context).put(context.requiredTestInstance, img)
        }

        private fun getStore(context: ExtensionContext): ExtensionContext.Store =
            context.getStore(ExtensionContext.Namespace.create(FixtureExtension::class))
    }
}

