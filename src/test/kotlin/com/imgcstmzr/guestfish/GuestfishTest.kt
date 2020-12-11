package com.imgcstmzr.guestfish

import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.withExtension
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.bkahlert.koodies.test.junit.debug.Debug
import com.imgcstmzr.guestfish.Guestfish.Companion.changePasswordCommand
import com.imgcstmzr.guestfish.Guestfish.Companion.copyInCommands
import com.imgcstmzr.guestfish.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.ImgFixture
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.hasEqualContent
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class GuestfishTest {

//    @Test
//    fun `copy-in should make paths absolute if relative`() {
//        val commands = copyInCommands(listOf(Path.of("foo/bar"), Path.of("/bar/baz")))
//        expectThat(commands).withCommands {
//            containsExactly(
//                "- mkdir-p /foo", "copy-in /shared/guestfish.shared/foo/bar /foo",
//                "- mkdir-p /bar", "copy-in /shared/guestfish.shared/bar/baz /bar",
//            )
//        }
//    }
//
//    @Test
//    fun `copy-out should make paths absolute if relative`() {
//        val commands = copyOutCommands(listOf(Path.of("foo/bar"), Path.of("/bar/baz")))
//        expectThat(commands).withCommands {
//            containsExactly(
//                "!mkdir -p /shared/guestfish.shared/foo", "- copy-out /foo/bar /shared/guestfish.shared/foo",
//                "!mkdir -p /shared/guestfish.shared/bar", "- copy-out /bar/baz /shared/guestfish.shared/bar",
//            )
//        }
//    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should copy file from osImage, skip non-existing and override one`(osImage: OperatingSystemImage, @Debug debug: Boolean) {
        val guestfish = Guestfish(osImage, this, debug = debug).withRandomSuffix()
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/cmdline.txt"), Path.of("/non/existing.txt"))))
        val dir = guestfish.guestRootOnHost

        dir.resolve("boot/config.txt").writeText("overwrite me")
        Guestfish(osImage, this).withRandomSuffix().run(copyOutCommands(listOf(Path.of("/boot/config.txt"))))

        expectThat(dir.resolve("boot/cmdline.txt")).hasContent(ImgFixture.Boot.CmdlineTxt.text)
        expectThat(dir.resolve("boot/config.txt")).hasContent(ImgFixture.Boot.ConfigTxt.text).not { hasContent("overwrite") }
    }

    @FiveMinutesTimeout @DockerRequiring @TestFactory
    fun InMemoryLogger.`should copy new file to osImage and overwrite a second one`(osImage: OperatingSystemImage) =
        listOf(
            "using copy-in" to { guestfish: Guestfish, guestPaths: List<Path> -> guestfish.run(copyInCommands(guestPaths)) },
            "using tar-in" to { guestfish: Guestfish, guestPaths: List<Path> -> guestfish.tarIn() },
        ).map { (name, copyInVariant) ->
            dynamicTest(name) {
                val guestfish = Guestfish(osImage, this).withRandomSuffix()
                val exampleHtml = Path.of("/home/user/example.html")
                val exampleHtmlOnHost = guestfish.guestRootOnHost.asRootFor(exampleHtml).also { ImgFixture.Home.User.ExampleHtml.copyTo(it) }
                val configTxt = Path.of("/boot/config.txt")
                val configTxtOnHost = guestfish.guestRootOnHost.asRootFor(configTxt).also { it.parent.mkdirs() }.writeText("overwrite guest")

                val guestPaths = listOf(exampleHtml, configTxt)
                copyInVariant(guestfish, guestPaths)
                exampleHtmlOnHost.delete(false)

                guestfish.run(copyOutCommands(listOf(exampleHtml, configTxt)))

                expectThat(exampleHtmlOnHost).hasContent(ImgFixture.Home.User.ExampleHtml.text)
                expectThat(configTxtOnHost).hasContent("overwrite guest").not { hasContent(ImgFixture.Boot.ConfigTxt.text) }
            }
        }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should change password`(@OS(DietPi) osImage: OperatingSystemImage) {
        val guestfish = Guestfish(osImage, this).withRandomSuffix()
        val shadowPath = Path.of("/etc/shadow")
        val hostShadow = guestfish.guestRootOnHost.asRootFor(shadowPath)

        guestfish.run(changePasswordCommand("root", String.random(), String.random(32)))
        expectThat(hostShadow).not { hasEqualContent(hostShadow.withExtension("bak")) }
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should update credentials password`(@OS(DietPi) osImage: OperatingSystemImage) {
        val guestfish = Guestfish(osImage, this).withRandomSuffix()
        val password = String.random()

        guestfish.run(changePasswordCommand("root", password, String.random(32)))

        expectThat(osImage.credentials).isEqualTo(OperatingSystem.Credentials("root", password))
    }
}
