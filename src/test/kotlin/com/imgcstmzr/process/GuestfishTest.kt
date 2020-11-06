package com.imgcstmzr.process

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.junit.debug.Debug
import com.imgcstmzr.process.Guestfish.Companion.changePasswordCommand
import com.imgcstmzr.process.Guestfish.Companion.copyInCommands
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.hasEqualContent
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.withCommands
import com.imgcstmzr.util.withExtension
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
class GuestfishTest {

    @Test
    fun `copy-in should make paths absolute if relative`() {
        val commands = copyInCommands(listOf(Path.of("foo/bar"), Path.of("/bar/baz")))
        expectThat(commands).withCommands {
            containsExactly("copy-in /work/guestfish.shared/foo/bar /foo", "copy-in /work/guestfish.shared/bar/baz /bar")
        }
    }

    @Test
    fun `copy-out should make paths absolute if relative`() {
        val commands = copyOutCommands(listOf(Path.of("foo/bar"), Path.of("/bar/baz")))
        expectThat(commands).withCommands {
            containsExactly(
                "!mkdir -p /work/guestfish.shared/foo", "- copy-out /foo/bar /work/guestfish.shared/foo",
                "!mkdir -p /work/guestfish.shared/bar", "- copy-out /bar/baz /work/guestfish.shared/bar",
            )
        }
    }

    @Test
    @DockerRequired
    fun `should copy file from osImage, skip non-existing and override one`(osImage: OperatingSystemImage, logger: InMemoryLogger<Any>, @Debug debug: Boolean) {
        val guestfish = Guestfish(osImage, logger, debug = debug).withRandomSuffix()
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/cmdline.txt"), Path.of("/non/existing.txt"))))
        val dir = guestfish.guestRootOnHost

        dir.resolve("boot/config.txt").writeText("overwrite me")
        Guestfish(osImage, logger).withRandomSuffix().run(copyOutCommands(listOf(Path.of("/boot/config.txt"))))

        expectThat(dir.resolve("boot/cmdline.txt")).hasEqualContent(ClassPath.of("cmdline.txt"))
        expectThat(dir.resolve("boot/config.txt")).hasEqualContent(ClassPath.of("config.txt")).not { hasContent("overwrite") }
    }

    @Test
    @DockerRequired
    fun `should copy new file to osImage and overwrite a second one`(osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val guestfish = Guestfish(osImage, logger).withRandomSuffix()
        val exampleHtml = Path.of("/example.html")
        val exampleHtmlOnHost = guestfish.guestRootOnHost.asRootFor(exampleHtml).also { ClassPath.of("example.html").copyTo(it) }
        val configTxt = Path.of("/boot/config.txt")
        val configTxtOnHost = guestfish.guestRootOnHost.asRootFor(configTxt).also { it.parent.mkdirs() }.also { it.writeText("overwrite guest") }

        val guestPaths = listOf(exampleHtml, configTxt)
        guestfish.run(copyInCommands(guestPaths))
        exampleHtmlOnHost.delete()

        guestfish.run(copyOutCommands(listOf(exampleHtml, configTxt)))

        expectThat(exampleHtmlOnHost).hasEqualContent(ClassPath.of("/example.html"))
        expectThat(configTxtOnHost).hasContent("overwrite guest").not { hasEqualContent(ClassPath.of("/config.txt")) }
    }

    @Test
    @DockerRequired
    fun `should change password`(@OS(DietPi::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val guestfish = Guestfish(osImage, logger).withRandomSuffix()
        val shadowPath = Path.of("/etc/shadow")
        val hostShadow = guestfish.guestRootOnHost.asRootFor(shadowPath)

        guestfish.run(changePasswordCommand("root", String.random(), String.random(32)))
        expectThat(hostShadow).not { hasEqualContent(hostShadow.withExtension("bak")) }
    }

    @Test
    @DockerRequired
    fun `should update credentials password`(@OS(DietPi::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val guestfish = Guestfish(osImage, logger).withRandomSuffix()
        val password = String.random()

        guestfish.run(changePasswordCommand("root", password, String.random(32)))

        expectThat(osImage.credentials).isEqualTo(OperatingSystems.Companion.Credentials("root", password))
    }
}
