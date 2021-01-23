package com.imgcstmzr.libguestfs.guestfish

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.mountRootForDisk
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.runGuestfishOn
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.test.DockerRequiring
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.ImgClassPathFixture
import com.imgcstmzr.test.ImgClassPathFixture.Home.User.ExampleHtml
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.containsContent
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.matchesCurlyPattern
import koodies.docker.asContainerPath
import koodies.io.path.asString
import koodies.io.path.delete
import koodies.io.path.writeText
import koodies.logging.BlockRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.logging.logging
import koodies.test.copyTo
import org.junit.jupiter.api.Nested
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
import kotlin.io.path.createDirectories

@Execution(CONCURRENT)
class GuestfishCommandLineTest {

    @Test
    fun `should be instantiatable`(osImage: OperatingSystemImage) {
        expectCatching { createGuestfishCommand(osImage) }.isSuccess()
    }


    @Nested
    inner class AsCommandLine {

        @Test
        fun `should use sibling shared dir as working dir`(osImage: OperatingSystemImage) {
            val cmdLine = createGuestfishCommand(osImage)
            expectThat(cmdLine.workingDirectory).isEqualTo(osImage.file.resolveSibling("shared"))
        }

        @Test
        fun `should have correctly mapped arguments`(osImage: OperatingSystemImage) {
            val cmdLine = createGuestfishCommand(osImage)
            expectThat(cmdLine.toString()).matchesCurlyPattern("""
                guestfish \
                --add \
                ${osImage.file.asString()} \
                --inspector \
                -- \
                <<HERE-{}
                !mkdir -p
                -mkdir-p /home/pi/.ssh 
                 -copy-in ${osImage.hostPath(DiskPath("home/pi/.ssh/known_hosts"))} /home/pi/.ssh 
                
                !mkdir -p ${osImage.hostPath(DiskPath("home/pi/.ssh"))} 
                 -copy-out /home/pi/.ssh/known_hosts ${osImage.hostPath(DiskPath("home/pi/.ssh"))} 
                
                tar-in ${osImage.hostPath(DiskPath("archive.tar"))} / 
                 !rm ${osImage.hostPath(DiskPath("archive.tar"))} 
                
                tar-out / ${osImage.hostPath(DiskPath("archive.tar"))} excludes:/archive.tar
                rm /rm
                rm-f /rm/force
                rm-rf /rm/force-recursive
                rm /rm/invalid-only-recursive
                rmdir /rm/dir
                umount-all
                exit
                HERE-{}
            """.trimIndent())
        }
    }

    @Nested
    inner class AsDockerCommandLine {

        @Test
        fun `should use sibling shared dir as working dir`(osImage: OperatingSystemImage) {
            val cmdLine = createGuestfishCommand(osImage).dockerCommandLine()
            expectThat(cmdLine.workingDirectory).isEqualTo(osImage.file.resolveSibling("shared"))
        }

        @Test
        fun `should use absolute shared dir as guest working dir`(osImage: OperatingSystemImage) {
            val cmdLine = createGuestfishCommand(osImage).dockerCommandLine()
            expectThat(cmdLine.options.workingDirectory).isEqualTo("/shared".asContainerPath())
        }

        @Test
        fun `should have correctly mapped arguments`(osImage: OperatingSystemImage) {
            val cmdLine = createGuestfishCommand(osImage).dockerCommandLine()
            expectThat(cmdLine.toString()).matchesCurlyPattern("""
                docker \
                run \
                --entrypoint \
                guestfish \
                --name \
                libguestfs-guestfish-${cmdLine.options.name.toString().takeLast(4)} \
                -w \
                /shared \
                --rm \
                -i \
                --mount \
                type=bind,source=${Libguestfs.mountRootForDisk(osImage.file)},target=/shared \
                --mount \
                type=bind,source=${osImage.file},target=/images/disk.img \
                bkahlert/libguestfs@sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a \
                --add \
                /images/disk.img \
                --inspector \
                -- \
                <<HERE-{}
                !mkdir -p
                -mkdir-p /home/pi/.ssh 
                 -copy-in home/pi/.ssh/known_hosts /home/pi/.ssh 
                
                !mkdir -p home/pi/.ssh 
                 -copy-out /home/pi/.ssh/known_hosts home/pi/.ssh 
                
                tar-in archive.tar / 
                 !rm archive.tar 
                
                tar-out / archive.tar excludes:/archive.tar
                rm /rm
                rm-f /rm/force
                rm-rf /rm/force-recursive
                rm /rm/invalid-only-recursive
                rmdir /rm/dir
                umount-all
                exit
                HERE-{}
            """.trimIndent())
        }
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should copy file from osImage, skip non-existing and override one`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        runGuestfishOn(osImage) {
            copyOut { DiskPath("/boot/cmdline.txt") }
            copyOut { DiskPath("/non/existing.txt") }
        }

        val dir = mountRootForDisk(osImage.file).apply {
            resolve("boot/config.txt").writeText("overwrite me")
        }

        runGuestfishOn(osImage) {
            copyOut { DiskPath("/boot/config.txt") }
        }

        expectThat(dir) {
            resolve("boot/cmdline.txt").hasContent(ImgClassPathFixture.Boot.CmdlineTxt.text)
            resolve("boot/config.txt").containsContent("# http://rpf.io/configtxt").containsContent("dtoverlay").not { containsContent("overwrite me") }
        }
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should copy new file to osImage and overwrite a second one`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        val exampleHtml = DiskPath("/home/user/example.html")
        val exampleHtmlOnHost = osImage.hostPath(exampleHtml).also { ExampleHtml.copyTo(it) }
        val configTxt = DiskPath("/boot/config.txt")
        val configTxtOnHost = osImage.hostPath(configTxt).apply { parent.createDirectories() }.apply { writeText("overwrite guest") }

        runGuestfishOn(osImage) { tarIn() }
        exampleHtmlOnHost.delete()

        runGuestfishOn(osImage) {
            copyOut { exampleHtml }
            copyOut { configTxt }
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
            copyIn { DiskPath("/home/pi/.ssh/known_hosts") }
        }
        copyOut { DiskPath("/home/pi/.ssh/known_hosts") }

        tarIn()
        tarOut()
        rm { DiskPath("/rm") }
        rm(force = true) { DiskPath("/rm/force") }
        rm(force = true, recursive = true) { DiskPath("/rm/force-recursive") }
        rm(force = false, recursive = true) { DiskPath("/rm/invalid-only-recursive") }
        rmDir { DiskPath("/rm/dir") }
        umountAll()
        exit()
    }
}


class GuestAssertions(private val assertions: MutableList<Pair<DiskPath, Assertion.Builder<HostPath>.() -> Unit>>) {
    fun path(path: String, assertion: Assertion.Builder<HostPath>.() -> Unit) = assertions.add(DiskPath(path) to assertion)
}

fun Assertion.Builder<OperatingSystemImage>.mounted(logger: BlockRenderingLogger, init: GuestAssertions.() -> Unit) =
    get("mounted") {
        // Getting paths and assertions
        val assertions = mutableListOf<Pair<DiskPath, Assertion.Builder<HostPath>.() -> Unit>>()
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
                            copyOut { path }
                        }
                    }
                }
            }.dockerCommandLine().run { execute() }
        }

        // Assert
        compose("with files $paths") {
            assertions.forEach { (path, assertion) ->
                get(path.toString()) { hostPath(path) }.assertion()
            }
        }.then { if (allPassed) pass() else fail() }
    }

