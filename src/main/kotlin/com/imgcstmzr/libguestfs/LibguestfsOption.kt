package com.imgcstmzr.libguestfs

import com.imgcstmzr.os.OperatingSystemImage
import koodies.io.path.pathString
import java.nio.file.Path

open class LibguestfsOption(open val name: String, open val arguments: List<String>) :
    List<String> by listOf(name) + arguments {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LibguestfsOption

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

    /**
     * Add a block device or virtual machine image to the shell.
     *
     * The format of the disk image is auto-detected.
     */
    interface DiskOption {
        val disk: Path
    }

    companion object {
        fun LibguestfsOption.relativize(disk: Path): LibguestfsOption =
            LibguestfsOption(name, arguments.map { relativize(disk, it) })

        private fun relativize(disk: Path, potentialPath: String): String {
            val mountRoot = OperatingSystemImage.mountRootForDisk(disk).pathString
            return potentialPath.takeUnless { it.startsWith(mountRoot) } ?: run {
                val diskAbsolute = potentialPath.removePrefix(mountRoot)
                val diskRelative = diskAbsolute.removePrefix("/")
                val sanitized = diskRelative.takeUnless { it.isEmpty() } ?: "."
                sanitized
            }
        }
    }
}
