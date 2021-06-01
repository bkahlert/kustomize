package com.imgcstmzr.libguestfs

import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.HostPath
import com.imgcstmzr.os.OperatingSystemImage
import koodies.collections.head
import koodies.collections.tail
import koodies.docker.asContainerPath
import koodies.io.path.delete
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess

class GuestfishCommandLineTest {

    @Test
    fun `should be instantiatable`(osImage: OperatingSystemImage) {
        expectCatching { createGuestfishCommand(osImage) }.isSuccess()
    }

    @Test
    fun `should use absolute shared dir as guest working dir`(osImage: OperatingSystemImage) {
        val cmdLine = createGuestfishCommand(osImage)
        expectThat(cmdLine.dockerOptions.workingDirectory).isEqualTo("/shared".asContainerPath())
    }

    @Test
    fun `should have correctly mapped arguments`(osImage: OperatingSystemImage) {
        val cmdLine = createGuestfishCommand(osImage)
        @Suppress("SpellCheckingInspection")
        expectThat(cmdLine).toStringMatchesCurlyPattern("""
            'docker' \
            'run' \
            '--entrypoint' \
            '/bin/sh' \
            '--name' \
            'guestfish--${cmdLine.dockerOptions.name!!.name.takeLast(4)}' \
            '--workdir' \
            '/shared' \
            '--rm' \
            '--interactive' \
            '--mount' \
            'type=bind,source=${osImage.exchangeDirectory},target=/shared' \
            '--mount' \
            'type=bind,source=${osImage.file},target=/images/disk.img' \
            'bkahlert/libguestfs@sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a' \
            '-c' \
            'guestfish --rw --add /images/disk.img --mount /dev/sda2:/ --mount /dev/sda1:/boot <<HERE-{}
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
            '
        """.trimIndent())
    }
}

internal fun createGuestfishCommand(osImage: OperatingSystemImage): GuestfishCommandLine = GuestfishCommandLine.build(osImage) {
    options {
        disk { it.file }
    }

    commands {
        custom("!mkdir", "-p")

        copyIn { DiskPath("/home/pi/.ssh/known_hosts") }
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
    fun path(diskPath: String, assertion: Assertion.Builder<HostPath>.() -> Unit) = assertions.add(DiskPath(diskPath) to assertion)
    fun path(diskPath: DiskPath, assertion: Assertion.Builder<HostPath>.() -> Unit) = assertions.add(diskPath to assertion)
}

/**
 * Returns callable that mounts `this` [OperatingSystemImage] while logging with `this` [RenderingLogger]
 * and runs the specified [GuestAssertions].
 */
inline val FixedWidthRenderingLogger.mounted: Assertion.Builder<OperatingSystemImage>.(init: GuestAssertions.() -> Unit) -> Unit
    get() = { init: GuestAssertions.() -> Unit -> mounted(this@mounted, init) }

/**
 * Returns callable that mounts `this` [OperatingSystemImage] while logging with the specified [logger]
 * and runs the specified [GuestAssertions].
 */
fun Assertion.Builder<OperatingSystemImage>.mounted(logger: FixedWidthRenderingLogger, init: GuestAssertions.() -> Unit) =
    get("mounted") {
        // Getting paths and assertions
        val assertions = mutableListOf<Pair<DiskPath, Assertion.Builder<HostPath>.() -> Unit>>()
        GuestAssertions(assertions).apply(init)
        val paths = assertions.map { it.first }

        // Copying out of VM
        logger.logLine { "" }
        logger.logging(
            "╶──╴copying ${paths.size} files out of $fileName for assertions".formattedAs.debug,
            decorationFormatter = { it.ansi.formattedAs.debug },
        ) {
            // delete to make sure the assertions runs on a file copy from the image
            paths.forEach { path -> hostPath(path).delete() }
            copyOut(paths.head.pathString, *paths.tail.map { it.pathString }.toTypedArray(), logger = MutedRenderingLogger)
        }

        // Assert
        compose("with files $paths") {
            assertions.forEach { (path, assertion) ->
                get(path.toString()) { hostPath(path) }.assertion()
            }
        }.then { if (allPassed) pass() else fail() }
    }
