package com.imgcstmzr.process

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.string.random
import com.imgcstmzr.process.Guestfish.Companion.changePasswordCommand
import com.imgcstmzr.process.Guestfish.Companion.copyInCommands
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.FixtureExtension
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.hasEqualContent
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.withExtension
import com.imgcstmzr.util.writeText
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
            "copy-in /work/guestfish.shared/foo/bar /foo", "copy-in /work/guestfish.shared/bar/baz /bar",
        )
    }

    @Test
    internal fun `copy-out should make paths absolute if relative`() {
        val commands = copyOutCommands(listOf(Path.of("foo/bar"), Path.of("/bar/baz")))
        expectThat(commands).containsExactly(
            "!mkdir -p /work/guestfish.shared/foo", "- copy-out /foo/bar /work/guestfish.shared/foo",
            "!mkdir -p /work/guestfish.shared/bar", "- copy-out /bar/baz /work/guestfish.shared/bar",
        )
    }

    @Test
    @DockerRequired
    internal fun `should copy file from img, skip non-existing and override one`(img: Path) {
        val guestfish = Guestfish(img).withRandomSuffix()
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/cmdline.txt"), Path.of("/non/existing.txt"))))
        val dir = guestfish.guestRootOnHost

        dir.resolve("boot/config.txt").writeText("overwrite me")
        Guestfish(img).withRandomSuffix().run(copyOutCommands(listOf(Path.of("/boot/config.txt"))))

        expectThat(dir.resolve("boot/cmdline.txt")).hasEqualContent(ClassPath.of("cmdline.txt"))
        expectThat(dir.resolve("boot/config.txt")).hasEqualContent(ClassPath.of("config.txt")).not { hasContent("overwrite") }
    }

    @Test
    @DockerRequired
    internal fun `should copy new file to img and overwrite a second one`(img: Path) {
        val guestfish = Guestfish(img).withRandomSuffix()
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
    @DockerRequired
    internal fun `should change password`(@OS(DietPi::class) img: Path) {
        val guestfish = Guestfish(img).withRandomSuffix()
        val shadowPath = Path.of("/etc/shadow")
        val hostShadow = guestfish.guestRootOnHost.asRootFor(shadowPath)

        expectCatching {
            check(guestfish.run(changePasswordCommand("root", String.random(), String.random(32))) == 0) { "An error occurred while setting the password" }
            Unit
        }.isSuccess()

        expectThat(hostShadow).not { hasEqualContent(hostShadow.withExtension("bak")) }
    }
}
