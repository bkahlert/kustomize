package com.imgcstmzr.libguestfs

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.docker.DockerRunAdaptable
import koodies.io.path.toPath
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

abstract class LibguestfsCommandLine(
    environment: Map<String, String>,
    workingDirectory: Path,
    command: String,
    arguments: List<String>,
) : CommandLine(emptyList(), environment, workingDirectory, command, arguments), DockerRunAdaptable {

    override fun execute(expectedExitValue: Int): ManagedProcess {
        val disk: Path = commandLineParts.dropWhile { it != "--add" }.drop(1).take(1).singleOrNull()?.toPath().run {
            checkNotNull(this) { "No included disk found." }
            check(exists()) { "Disk $this does no exist." }
            check(isReadable()) { "Disk $this is not readable." }
            check(isWritable()) { "Disk $this is not writable." }
            this
        }

        val sharedDir = disk.resolveSibling("shared")
        sharedDir.createDirectories()
        return adapt().execute(expectedExitValue)
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
