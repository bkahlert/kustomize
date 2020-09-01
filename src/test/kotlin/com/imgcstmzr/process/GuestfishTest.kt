package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.util.ClassPath
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasEqualContent
import com.imgcstmzr.util.random
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import java.nio.file.Files
import java.nio.file.Files.isDirectory
import java.nio.file.Path

val userTempDir = Path.of(System.getProperty("user.home")).resolve("tmp")

@Suppress("RedundantInnerClassModifier")
@Execution(ExecutionMode.CONCURRENT)
internal class GuestfishTest {
    @Test
    internal fun `should copy file from img`() {
        val img = prepareImg("cmdline.txt", "config.txt")

        val dir = Guestfish(img.fileName.toString(), img).copyFromGuest(listOf(Path.of("/boot/cmdline.txt")))

        expectThat(dir.resolve("boot/cmdline.txt")).hasEqualContent(ClassPath.of("cmdline.txt"))
    }

    @Test
    internal fun `should copy file to img`() {
        val img = prepareImg("cmdline.txt", "config.txt")
        val guestfish = Guestfish(img.fileName.toString(), img)
        val exampleHtml = Path.of("/example.html")
        val exampleHtmlOnHost = guestfish.hostPath(exampleHtml)
        ClassPath.of("example.html").copy(exampleHtmlOnHost)

        guestfish.copyToGuest(listOf(exampleHtml))
        exampleHtmlOnHost.delete()

        val exampleHtmlCopiedBack = guestfish.copyFromGuest(listOf(exampleHtml)).resolve("example.html")

        expectThat(exampleHtmlCopiedBack).hasEqualContent(ClassPath.of("/example.html"))
    }

    @Test
    @Disabled
    internal fun `should provide access to filesystem`() {
        // @see https://medium.com/@kumar_pravin/mount-disk-image-to-host-using-guestfish-d5f33c0297e0
        val dir = userTempDir.resolve("experiment").toAbsolutePath()
        // TODO since bootup takes so long, while developing stick to a cached copy of the img to be mounted
        val img: Path = dir.resolve("experiment.img").takeIf { it.toFile().exists() }
            ?: prepareImg("cmdline.txt", "config.txt").toFile().copyTo(dir.resolve("experiment.img").toFile()).toPath()

        val imgName = "experiment.img"
        Guestfish.execute(
            containerName = imgName,
            volumes = mapOf(dir to Path.of("/root"), img to Path.of("/root").resolve(imgName)),
            "add /root/$imgName",
            "run",
            "modprobe fuse",
            "mount /dev/sda2 /",
            "mount /dev/sda1 /boot",
            "mounts",
//            "!mkdir -p /root/mnt",
            "mount-local /root readonly:true",
            "mount-local-run",
        )
    }

    @Suppress("unused")
    @AfterAll
    internal fun cleanUp() {
        Files.list(userTempDir)
            .filter { isDirectory(it) && it.fileName.toString().startsWith("guestfish-test-") }
            .forEach { it.delete() }
    }
}


/**
 * Dynamically creates a raw image with two partitions containing the given [files].
 */
private fun prepareImg(vararg files: String): Path {
    val randomBasename = "guestfish-test-${String.random()}"
    echo(tc.gray("Preparing test img with files ${files.joinToString(", ")}. This takes a moment..."))
    val hostDir = userTempDir.resolve(randomBasename).toAbsolutePath()
    val copyFileCommands: List<String> = files.map {
        ClassPath.of(it).copy(hostDir.resolve(it))
        "copy-in /root/$it /boot"
    }


    val imgName = "$randomBasename.img"
    Guestfish.execute(
        containerName = imgName,
        volumes = mapOf(hostDir to Path.of("/root")),
        "sparse /root/$imgName 4M", // = 8192 sectors
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
    echo(tc.gray("Finish test img creation."))
    return hostDir.resolve(imgName)
}
