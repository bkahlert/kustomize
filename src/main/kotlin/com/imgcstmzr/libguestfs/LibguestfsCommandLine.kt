package com.imgcstmzr.libguestfs

import com.github.ajalt.mordant.AnsiColorCode
import com.imgcstmzr.libguestfs.Libguestfs.Companion.mountRootForDisk
import koodies.builder.OnOffBuilder
import koodies.concurrent.process.CommandLine
import koodies.docker.DockerCommandLine
import koodies.docker.DockerCommandLineOptionsBuilder
import koodies.docker.DockerImage
import koodies.docker.DockerImageBuilder
import koodies.io.path.asString
import koodies.io.path.withDirectoriesCreated
import koodies.logging.RenderingLogger
import koodies.text.withRandomSuffix
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

abstract class LibguestfsCommandLine(
    environment: Map<String, String>,
    disk: Path,
    command: String,
    arguments: List<String>,
) : CommandLine(listOf("2>&1"), environment, mountRootForDisk(disk), command, arguments) {

    abstract val disk: Path

    fun dockerCommandLine(): DockerCommandLine {
        val disk: Path = disk.run {
            checkNotNull(this) { "No included disk found." }
            check(exists()) { "Disk $this does no exist." }
            check(isReadable()) { "Disk $this is not readable." }
            check(isWritable()) { "Disk $this is not writable." }
            this
        }
        return DockerCommandLine(DOCKER_IMAGE, DockerCommandLineOptionsBuilder.build {
            entrypoint { command }
            name { "libguestfs-${command}".withRandomSuffix() }
            autoCleanup { OnOffBuilder.on }
            mounts {
                with(disk) {
                    workingDirectory mountAt "/shared"
                    this mountAt "/images/disk.img"
                }
            }
        }, this.run {
            CommandLine(redirects, environment, workingDirectory.withDirectoriesCreated(), command, arguments
                .map {
                    // converts absolute paths like /host/dir/shared/var/log to var/log
                    it.replace(workingDirectory.asString() + "/", "")
                })
        })
    }

    override fun RenderingLogger.executeLogging(
        caption: String,
        bordered: Boolean,
        ansiCode: AnsiColorCode,
        nonBlockingReader: Boolean,
        expectedExitValue: Int,
    ): Int = with(dockerCommandLine()) {
        executeLogging(caption, bordered, ansiCode, nonBlockingReader, expectedExitValue)
    }

    companion object {
        val DOCKER_IMAGE: DockerImage =
            DockerImageBuilder.build { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }
    }
}

open class Option(open val name: String, open val arguments: List<String>) :
    List<String> by listOf(name) + arguments {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Option

        if (name != other.name) return false
        if (arguments != other.arguments) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + arguments.hashCode()
        return result
    }

    override fun toString(): String {
        val simpleName = this::class.simpleName ?: "GuestfishCommand"
        val cmd = simpleName.replace("Guestfish", "><> ").replace("Command", "")
        return "$cmd(${joinToString("; ")})"
    }
}

/**
 * Add a block device or virtual machine image to the shell.
 *
 * The format of the disk image is auto-detected.
 */
interface DiskOption {
    val disk: Path
}
