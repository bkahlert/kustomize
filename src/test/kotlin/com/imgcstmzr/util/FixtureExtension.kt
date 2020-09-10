package com.imgcstmzr.util

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Downloader
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.OS
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.nio.file.Path

class FixtureExtension : ParameterResolver, AfterEachCallback {

    val baseImg = Paths.TEST
        .resolve("test")
        .mkdirs()
        .resolve("imgcstmzr.img")
        .also { if (!it.exists) prepareImg("cmdline.txt", "config.txt").copyTo(it) }

    companion object {

        /**
         * Dynamically creates a raw image with two partitions containing the given [files].
         */
        private fun prepareImg(vararg files: String): Path {
            val randomBasename = "img-${String.random()}"
            echo(META typed "Preparing test img with files ${files.joinToString(", ")}. This takes a moment...")
            val hostDir = Paths.TEST.resolve(randomBasename).mkdirs()
            val copyFileCommands: List<String> = files.map {
                ClassPath.of(it).copyTo(hostDir.resolve(it))
                "copy-in ${Guestfish.DOCKER_MOUNT_ROOT.resolve(it)} /boot"
            }

            val imgName = "$randomBasename.img"
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

        fun downloadImg(os: OS<*>): Path {
            val randomBasename = "$os-${String.random()}"
            echo(META typed "Preparing test img with OS $os. This takes a moment...")
            val hostDir = File.createTempFile("imgcstmzr", ".img").also { it.delete() }.toPath().resolve(randomBasename).mkdirs()
            return Downloader.download(os.downloadUrl ?: throw IllegalArgumentException("$os does not provide a download URL"))
                .also { echo(META typed "Finish download.") }
                .copyTo(hostDir.resolve("$randomBasename.img"))
        }

        fun prepareRoot(): Path {
            val root = createTempDir("imgcstmzr")
            val boot = root.resolve("boot").toPath()
            ClassPath.of("cmdline.txt").copyToDirectory(boot)
            ClassPath.of("config.txt").copyToDirectory(boot)
            return root.toPath()
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?): Boolean =
        Path::class.java.isAssignableFrom(parameterContext.parameter.type)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?): Any? =
        baseImg.copyToTempSiblingDirectory()
            .also { setUp(extensionContext ?: throw NoSuchElementException(), it) }

    private fun setUp(context: ExtensionContext, path: Path) {
        saveImg(context, path)
    }

    override fun afterEach(context: ExtensionContext) {
        cleanUp(context)
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

    private fun saveImg(context: ExtensionContext, img: Path) {
        getStore(context).put(context.requiredTestInstance, img)
    }

    private fun getStore(context: ExtensionContext): ExtensionContext.Store =
        context.getStore(ExtensionContext.Namespace.create(FixtureExtension::class))
}
