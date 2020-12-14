package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.runGuestfishOn
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.libguestfs.resolveOnDocker
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.logging
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.ImgFixture
import com.imgcstmzr.util.ImgFixture.Home.User.ExampleHtml
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.containsContent
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.resolve
import java.nio.file.Path

@Execution(CONCURRENT)
class GuestfishCommandLineTest {

    @Test
    fun `should be instantiatable`(osImage: OperatingSystemImage) {
        expectCatching { createGuestfishCommand(osImage) }.isSuccess()
    }

    @Test
    fun `should build proper command line even arguments by default`(osImage: OperatingSystemImage) {
        val commandLine = createGuestfishCommand(osImage)
        expectThat(commandLine.toString()).isEqualTo("""
            guestfish \
            --add \
            ${osImage.file.serialized} \
            --inspector \
            !mkdir \
            -p \
            -mkdir-p \
            /home/pi/.ssh \
             \
            -copy-in \
            /shared/home/pi/.ssh/known_hosts \
            /home/pi/.ssh \
             \
            !mkdir \
            -p \
            /shared/home/pi/.ssh \
             \
            -copy-out \
            /home/pi/.ssh/known_hosts \
            /shared/home/pi/.ssh \
             \
            tar-in \
            /shared/archive.tar \
            / \
             \
            !rm \
            /shared/archive.tar \
             \
            tar-out \
            / \
            /shared/archive.tar \
            excludes:/archive.tar \
            rm \
            /rm \
            rm-f \
            /rm/force \
            rm-rf \
            /rm/force-recursive \
            rm \
            /rm/invalid-only-recursive \
            rmdir \
            /rm/dir \
            umount-all \
            exit
        """.trimIndent())
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should copy file from osImage, skip non-existing and override one`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        runGuestfishOn(osImage) {
            copyOut { it.resolveOnDisk("/boot/cmdline.txt") }
            copyOut { it.resolveOnDisk("/non/existing.txt") }
        }

        val dir = osImage.resolveOnHost("").apply {
            resolve("boot/config.txt").writeText("overwrite me")
        }

        runGuestfishOn(osImage) {
            copyOut { it.resolveOnDisk("/boot/config.txt") }
        }

        expectThat(dir) {
            resolve("boot/cmdline.txt").hasContent(ImgFixture.Boot.CmdlineTxt.text)
            resolve("boot/config.txt").containsContent("# http://rpf.io/configtxt").containsContent("dtoverlay").not { containsContent("overwrite me") }
        }
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should copy new file to osImage and overwrite a second one`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        val exampleHtml = f("/home/user/example.html")
        val exampleHtmlOnHost = osImage.resolveOnHost(exampleHtml).also { ExampleHtml.copyTo(it) }
        val configTxt = f("/boot/config.txt")
        val configTxtOnHost = osImage.resolveOnHost(configTxt).apply { parent.mkdirs() }.writeText("overwrite guest")

        runGuestfishOn(osImage) { tarIn() }
        exampleHtmlOnHost.delete(false)

        runGuestfishOn(osImage) {
            copyOut { it.resolveOnDisk(exampleHtml) }
            copyOut { it.resolveOnDisk(configTxt) }
        }

        expect {
            that(exampleHtmlOnHost).hasContent(ExampleHtml.text)
            that(configTxtOnHost).hasContent("overwrite guest").not { hasContent(ImgFixture.Boot.ConfigTxt.text) }
        }
    }
}

private fun f(path: String): Path = Path.of(path)

internal fun createGuestfishCommand(osImage: OperatingSystemImage) = GuestfishCommandLine.build(osImage) {
    options {
        disk { it.file }
        inspector { on }
    }

    commands {
        runLocally {
            command("mkdir", "-p")
        }

        ignoreErrors {
            copyIn { it.resolveOnDocker("/home/pi/.ssh/known_hosts") }
        }
        copyOut { it.resolveOnDisk("/home/pi/.ssh/known_hosts") }

        tarIn()
        tarOut()
        rm { it.resolveOnDisk("/rm") }
        rm(force = true) { it.resolveOnDisk("/rm/force") }
        rm(force = true, recursive = true) { it.resolveOnDisk("/rm/force-recursive") }
        rm(force = false, recursive = true) { it.resolveOnDisk("/rm/invalid-only-recursive") }
        rmDir { it.resolveOnDisk("/rm/dir") }
        umountAll()
        exit()
    }
}


class GuestAssertions(private val assertions: MutableList<Pair<Path, Assertion.Builder<Path>.() -> Unit>>) {
    fun path(path: String, assertion: Assertion.Builder<Path>.() -> Unit) = assertions.add(path.toPath() to assertion)
}

fun Assertion.Builder<OperatingSystemImage>.mounted(logger: BlockRenderingLogger, init: GuestAssertions.() -> Unit) =
    get("mounted") {
        // Getting paths and assertions
        val assertions = mutableListOf<Pair<Path, Assertion.Builder<Path>.() -> Unit>>()
        GuestAssertions(assertions).apply(init)
        val paths = assertions.map { it.first }

        // Copying out of VM
        logger.logging("copying ${paths.size} files out of $fileName for assertions") {
            GuestfishCommandLine.build(this@get) {
                options {
                    disk { it.file }
                    inspector { on }
                }
                paths.forEach { path ->
                    commands {
                        ignoreErrors {
                            copyOut { it.resolveOnDisk(path) }
                        }
                    }
                }
            }.execute(this)
        }

        // Assert
        compose("with files $paths") {
            assertions.forEach { (path, assertion) ->
                get(path.serialized) { resolveOnHost(path) }.assertion()
            }
        }.then { if (allPassed) pass() else fail() }
    }

