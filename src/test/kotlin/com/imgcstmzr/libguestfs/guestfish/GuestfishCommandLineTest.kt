package com.imgcstmzr.libguestfs.guestfish

import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.runGuestfishOn
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.libguestfs.resolveOnDocker
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.test.DockerRequiring
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.ImgClassPathFixture
import com.imgcstmzr.test.ImgClassPathFixture.Home.User.ExampleHtml
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.containsContent
import com.imgcstmzr.test.hasContent
import koodies.io.path.asString
import koodies.io.path.delete
import koodies.io.path.toPath
import koodies.io.path.writeText
import koodies.logging.BlockRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.logging.logging
import koodies.test.copyTo
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
import kotlin.io.path.createDirectories

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
            ${osImage.file.asString()} \
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
            resolve("boot/cmdline.txt").hasContent(ImgClassPathFixture.Boot.CmdlineTxt.text)
            resolve("boot/config.txt").containsContent("# http://rpf.io/configtxt").containsContent("dtoverlay").not { containsContent("overwrite me") }
        }
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should copy new file to osImage and overwrite a second one`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        val exampleHtml = "/home/user/example.html".toPath()
        val exampleHtmlOnHost = osImage.resolveOnHost(exampleHtml).also { ExampleHtml.copyTo(it) }
        val configTxt = "/boot/config.txt".toPath()
        val configTxtOnHost = osImage.resolveOnHost(configTxt).apply { parent.createDirectories() }.apply { writeText("overwrite guest") }

        runGuestfishOn(osImage) { tarIn() }
        exampleHtmlOnHost.delete()

        runGuestfishOn(osImage) {
            copyOut { it.resolveOnDisk(exampleHtml) }
            copyOut { it.resolveOnDisk(configTxt) }
        }

        expect {
            that(exampleHtmlOnHost).hasContent(ExampleHtml.text)
            that(configTxtOnHost).hasContent("overwrite guest").not { hasContent(ImgClassPathFixture.Boot.ConfigTxt.text) }
        }
    }
}

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
            }.run { execute() }
        }

        // Assert
        compose("with files $paths") {
            assertions.forEach { (path, assertion) ->
                get(path.asString()) { resolveOnHost(path) }.assertion()
            }
        }.then { if (allPassed) pass() else fail() }
    }

