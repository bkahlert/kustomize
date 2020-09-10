package com.imgcstmzr.process

import com.imgcstmzr.cli.Cache
import com.imgcstmzr.process.Guestfish.Companion.changePasswordCommand
import com.imgcstmzr.process.Guestfish.Companion.copyInCommands
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.DietPi
import com.imgcstmzr.util.ClassPath
import com.imgcstmzr.util.FixtureExtension
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.hasEqualContent
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.random
import com.imgcstmzr.util.withExtension
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isSuccess
import java.nio.file.Path


@Suppress("RedundantInnerClassModifier")
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(FixtureExtension::class)
internal class GuestfishTest {

    @Test
    internal fun `copy-in should make paths absolute if relative`() {
        val commands = copyInCommands(listOf(Path.of("foo/bar"), Path.of("/bar/baz")))
        expectThat(commands).containsExactly(
            "copy-in /root/guestfish.shared/foo/bar /foo", "copy-in /root/guestfish.shared/bar/baz /bar",
        )
    }

    @Test
    internal fun `copy-out should make paths absolute if relative`() {
        val commands = copyOutCommands(listOf(Path.of("foo/bar"), Path.of("/bar/baz")))
        expectThat(commands).containsExactly(
            "!mkdir -p /root/guestfish.shared/foo", "- copy-out /foo/bar /root/guestfish.shared/foo",
            "!mkdir -p /root/guestfish.shared/bar", "- copy-out /bar/baz /root/guestfish.shared/bar",
        )
    }

    @Test
    internal fun `should copy file from img, skip non-existing and override one`(img: Path) {
        val guestfish = Guestfish(img)
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/cmdline.txt"), Path.of("/non/existing.txt"))))
        val dir = guestfish.guestRootOnHost

        dir.resolve("boot/config.txt").writeText("overwrite me")
        Guestfish(img, img.fileName.toString()).run(copyOutCommands(listOf(Path.of("/boot/config.txt"))))

        expectThat(dir.resolve("boot/cmdline.txt")).hasEqualContent(ClassPath.of("cmdline.txt"))
        expectThat(dir.resolve("boot/config.txt")).hasEqualContent(ClassPath.of("config.txt")).not { hasContent("overwrite") }
    }

    @Test
    internal fun `should copy new file to img and overwrite a second one`(img: Path) {
        val guestfish = Guestfish(img)
        val exampleHtml = Path.of("/example.html")
        val exampleHtmlOnHost = guestfish.guestRootOnHost.asRootFor(exampleHtml).also { ClassPath.of("example.html").copyTo(it) }
        val configTxt = Path.of("/boot/config.txt")
        val configTxtOnHost = guestfish.guestRootOnHost.asRootFor(configTxt).also { it.parent.mkdirs() }.also { it.writeText("overwrite guest") }

        val guestPaths = listOf(exampleHtml, configTxt)
        check(guestfish.run(copyInCommands(guestPaths)) == 0) { "An error occurred while copying ${guestPaths.size} files to $img" }
        exampleHtmlOnHost.delete()

        guestfish.run(copyOutCommands(listOf(exampleHtml, configTxt)))

        expectThat(exampleHtmlOnHost).hasEqualContent(ClassPath.of("/example.html"))
        expectThat(configTxtOnHost).hasContent("overwrite guest").not { hasEqualContent(ClassPath.of("/config.txt")) }
    }

    @Test
    @Disabled
    internal fun `should provide access to filesystem`(img: Path) {
        // @see https://medium.com/@kumar_pravin/mount-disk-image-to-host-using-guestfish-d5f33c0297e0

        val imgName = img.fileName.toString()
        Guestfish.execute(
            containerName = imgName,
            volumes = mapOf(img.parent to Guestfish.DOCKER_MOUNT_ROOT, img to Guestfish.DOCKER_MOUNT_ROOT.resolve(imgName)),
            "add " + Guestfish.DOCKER_MOUNT_ROOT.resolve(imgName),
            "run",
            "modprobe fuse",
            "mount /dev/sda2 /",
            "mount /dev/sda1 /boot",
            "mounts",
//            "!mkdir -p " + Guestfish.dockerMountRoot.resolve(imgName).resolve("/mnt"),
            "mount-local ${Guestfish.DOCKER_MOUNT_ROOT} readonly:true",
            "mount-local-run",
        )
    }

    @Test
    internal fun `should change password`() {
        val img = Cache(Paths.CACHE.resolve("dietpi")).provideCopy("dietpi", true) { Downloader.download(DietPi().downloadUrl) }
        val guestfish = Guestfish(img)
        val shadowPath = Path.of("/etc/shadow")
        val hostShadow = guestfish.guestRootOnHost.asRootFor(shadowPath)

        expectCatching {
            check(guestfish.run(changePasswordCommand("root", String.random(), String.random(32))) == 0) { "An error occurred while setting the password" }
            Unit
        }.isSuccess()

        expectThat(hostShadow).not { hasEqualContent(hostShadow.withExtension("bak")) }
    }
}
