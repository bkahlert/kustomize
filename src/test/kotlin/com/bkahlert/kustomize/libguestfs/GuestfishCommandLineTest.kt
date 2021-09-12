package com.bkahlert.kustomize.libguestfs

import com.bkahlert.kommons.collections.head
import com.bkahlert.kommons.collections.tail
import com.bkahlert.kommons.docker.asContainerPath
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.LineSeparators.CR
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.toStringMatchesCurlyPattern
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kustomize.Kustomize
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishOptions
import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.LinuxRoot.etc
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.patch.user
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
            'bkahlert/libguestfs@sha256:de20843ae800c12a8b498c10ec27e2136b55dee4d62d927dff6b3ae360676d00' \
            '-c' \
            'guestfish --rw --add /images/disk.img --mount /dev/sda2:/ --mount /dev/sda1:/boot <<HERE-{}
            !mkdir -p
            -mkdir-p /home/pi/.ssh 
             -copy-in home/pi/.ssh/known_hosts /home/pi/.ssh 
            
            !mkdir -p home/pi/.ssh 
             -copy-out /home/pi/.ssh/known_hosts home/pi/.ssh
             !chown -R ${Kustomize.user} home/pi/.ssh
            
            -write-append /etc/sudoers.d/privacy "\x44\x65\x66\x61\x75\x6c\x74\x73\x20\x20\x20\x20\x20\x20\x20\x20\x6c\x65\x63\x74\x75\x72\x65\x20\x3d\x20\x6e\x65\x76\x65\x72\x0d"
            -write-append /etc/sudoers.d/privacy "\x44\x65\x66\x61\x75\x6c\x74\x73\x20\x20\x20\x20\x20\x20\x20\x20\x6c\x65\x63\x74\x75\x72\x65\x20\x3d\x20\x6e\x65\x76\x65\x72\x0a"
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
        copyOut { LinuxRoot.home / "pi" / ".ssh" / "known_hosts" }//sudoers.d
        writeAppend(etc.sudoers_d.privacy, "Defaults        lecture = never$CR")
        writeAppendLine(etc.sudoers_d.privacy, "Defaults        lecture = never")

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
        runSpanning(
            "copying ${paths.size} files out of $fileName for assertions".formattedAs.debug,
            decorationFormatter = { it.ansi.formattedAs.debug },
        ) {
            // delete to make sure the assertions runs on a file copy from the image
            paths.forEach { path -> hostPath(path).deleteRecursively() }
            copyOut(paths.head.pathString, *paths.tail.map { it.pathString }.toTypedArray())
        }

        // Assert
        compose("with files $paths") {
            assertions.forEach { (path, assertion) ->
                get(path.toString()) { hostPath(path) }.assertion()
            }
        }.then { if (allPassed) pass() else fail() }
    }
