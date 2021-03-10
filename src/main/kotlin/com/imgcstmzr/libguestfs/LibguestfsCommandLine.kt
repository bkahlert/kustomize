package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.Libguestfs.Companion.mountRootForDisk
import koodies.concurrent.process.CommandLine
import koodies.docker.Docker
import koodies.docker.DockerImage
import koodies.docker.DockerRunCommandLine
import koodies.io.path.asString
import koodies.io.path.withDirectoriesCreated
import koodies.text.withRandomSuffix
import koodies.text.withoutPrefix
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

abstract class LibguestfsCommandLine(
    environment: Map<String, String>,
    disk: Path,
    command: String,
    arguments: List<String>,
) : CommandLine(
    redirects = listOf("2>&1"),
    environment = environment,
    workingDirectory = mountRootForDisk(disk),
    command = command,
    arguments = arguments
) {

    abstract val disk: Path

    fun dockerCommandLine(): DockerRunCommandLine {
        val disk: Path = disk.run {
            checkNotNull(this) { "No included disk found." }
            check(exists()) { "Disk $this does no exist." }
            check(isReadable()) { "Disk $this is not readable." }
            check(isWritable()) { "Disk $this is not writable." }
            this
        }

        val cmdLine = CommandLine(
            redirects,
            environment,
            workingDirectory.withDirectoriesCreated(),
            command,
            arguments,
        )

        return DockerRunCommandLine {
            image by DOCKER_IMAGE
            options {
                entrypoint { command }
                name { "libguestfs-${command}".withRandomSuffix() }
                autoCleanup { on }
                mounts {
                    cmdLine.workingDirectory mountAt "/shared"
                    disk mountAt "/images/disk.img"
                }
            }
            commandLine by cmdLine
        }
    }

    companion object {
        val DOCKER_IMAGE: DockerImage =
            Docker.image { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }

        fun relativize(disk: Path, potentialPath: String): String {
            val mountRoot = mountRootForDisk(disk).asString()
            return potentialPath.takeUnless { it.startsWith(mountRoot) } ?: run {
                val diskAbsolute = potentialPath.withoutPrefix(mountRoot)
                val diskRelative = diskAbsolute.withoutPrefix("/")
                val sanitized = diskRelative.takeUnless { it.isEmpty() } ?: "."
                sanitized
            }
        }
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
