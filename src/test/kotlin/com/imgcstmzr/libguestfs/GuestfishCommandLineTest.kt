package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOptions
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import koodies.collections.head
import koodies.collections.tail
import koodies.docker.asContainerPath
import koodies.io.path.delete
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs
import koodies.text.toStringMatchesCurlyPattern
import koodies.tracing.spanning
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import java.nio.file.Path

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
            'guestfish--${(cmdLine.dockerOptions.name ?: error("Missing name")).name.takeLast(4)}' \
            '--workdir' \
            '/shared' \
            '--rm' \
            '--interactive' \
            '--mount' \
            'type=bind,source=${osImage.exchangeDirectory},target=/shared' \
            '--mount' \
            'type=bind,source=${osImage.file},target=/images/disk.img' \
            'bkahlert/libguestfs@sha256:e8fdf16c69a9155b0e30cdc9b2f872232507f5461be2e7dff307f4c1b50faa20' \
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

internal fun createGuestfishCommand(osImage: OperatingSystemImage): GuestfishCommandLine = GuestfishCommandLine(
    GuestfishOptions(osImage.file),
    GuestfishCommandsBuilder(osImage).build {
        custom("!mkdir", "-p")

        copyIn { LinuxRoot.home / "pi" / ".ssh" / "known_hosts" }
        copyOut { LinuxRoot.home / "pi" / ".ssh" / "known_hosts" }

        tarIn()
        tarOut()
        rm { LinuxRoot / "rm" }
        rm(force = true) { LinuxRoot / "rm" / "force" }
        rm(force = true, recursive = true) { LinuxRoot / "rm" / "force-recursive" }
        rm(force = false, recursive = true) { LinuxRoot / "rm" / "invalid-only-recursive" }
        rmDir { LinuxRoot / "rm" / "dir" }
        umountAll()
        exit()
    },
)


class GuestAssertions(private val assertions: MutableList<Pair<DiskPath, Assertion.Builder<Path>.() -> Unit>>) {
    fun path(diskPath: String, assertion: Assertion.Builder<Path>.() -> Unit) = assertions.add(LinuxRoot / diskPath to assertion)
    fun path(diskPath: DiskPath, assertion: Assertion.Builder<Path>.() -> Unit) = assertions.add(diskPath to assertion)
}

/**
 * Returns callable that mounts `this` [OperatingSystemImage]
 * and runs the specified [GuestAssertions].
 */
inline val mounted: Assertion.Builder<OperatingSystemImage>.(init: GuestAssertions.() -> Unit) -> Unit
    get() = { init: GuestAssertions.() -> Unit -> mounted(init) }

/**
 * Returns callable that mounts `this` [OperatingSystemImage] and runs the specified [GuestAssertions].
 */
fun Assertion.Builder<OperatingSystemImage>.mounted(init: GuestAssertions.() -> Unit) =
    get("mounted") {
        // Getting paths and assertions
        val assertions = mutableListOf<Pair<DiskPath, Assertion.Builder<Path>.() -> Unit>>()
        GuestAssertions(assertions).apply(init)
        val paths = assertions.map { it.first }

        // Copying out of VM
        spanning(
            "╶──╴copying ${paths.size} files out of $fileName for assertions".formattedAs.debug,
            decorationFormatter = { it.ansi.formattedAs.debug },
        ) {
            // delete to make sure the assertions runs on a file copy from the image
            paths.forEach { path -> hostPath(path).delete() }
            copyOut(paths.head.pathString, *paths.tail.map { it.pathString }.toTypedArray())
        }

        // Assert
        compose("with files $paths") {
            assertions.forEach { (path, assertion) ->
                get(path.toString()) { hostPath(path) }.assertion()
            }
        }.then { if (allPassed) pass() else fail() }
    }
