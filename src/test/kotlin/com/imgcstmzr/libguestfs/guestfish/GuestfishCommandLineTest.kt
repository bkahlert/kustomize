package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.fishGuest
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
    fun `should break even arguments by default`(osImage: OperatingSystemImage) {
        expectThat(createGuestfishCommand(osImage).toString()).isEqualTo("""
            guestfish \
            --add \
            my/disk.img \
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
            rm-rf \
            /rm/invalid-only-recursive \
            rmdir \
            /rm/dir \
            umount-all \
            exit
        """.trimIndent())
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should copy file from osImage, skip non-existing and override one`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        fishGuest(osImage) {
            copyOut(f("/boot/cmdline.txt"))
            copyOut(f("/non/existing.txt"))
        }

        val dir = osImage.resolveOnHost("").apply {
            resolve("boot/config.txt").writeText("overwrite me")
        }

        fishGuest(osImage) {
            copyOut(f("/boot/config.txt"))
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

        fishGuest(osImage) {
            tarIn(osImage)
        }
        exampleHtmlOnHost.delete(false)

        fishGuest(osImage) {
            copyOut(exampleHtml)
            copyOut(configTxt)
        }

        expect {
            that(exampleHtmlOnHost).hasContent(ExampleHtml.text)
            that(configTxtOnHost).hasContent("overwrite guest").not { hasContent(ImgFixture.Boot.ConfigTxt.text) }
        }
    }
}

private fun f(path: String): Path = Path.of(path)

internal fun createGuestfishCommand(osImage: OperatingSystemImage) = GuestfishCommandLine.Companion.build {
    options {
        disks { +Path.of("my/disk.img") }
        inspector { on }
    }

    commands {
        runLocally {
            command("mkdir", "-p")
        }

        ignoreErrors {
            copyIn(f("/home/pi/.ssh/known_hosts"))
        }
        copyOut(f("/home/pi/.ssh/known_hosts"))

        tarIn(osImage)
        tarOut(osImage)
        rm(f("/rm"))
        rm(f("/rm/force"), force = true)
        rm(f("/rm/force-recursive"), force = true, recursive = true)
        rm(f("/rm/invalid-only-recursive"), force = true, recursive = true)
        rmDir(f("/rm/dir"))
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
            GuestfishCommandLine.build {
                options {
                    disks { +GuestfishOption.DiskOption(file) }
                    inspector { on }
                }
                paths.forEach {
                    commands {
                        ignoreErrors {
                            copyOut(it)
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

