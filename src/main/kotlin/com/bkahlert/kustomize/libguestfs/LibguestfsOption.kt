package com.bkahlert.kustomize.libguestfs

import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Option.DiskOption
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.util.DiskUtil.disks
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.properties.ReadOnlyProperty

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

        companion object {
            fun resolveDisk(diskOptions: List<DiskOption>): ReadOnlyProperty<Any?, Path> =
                ReadOnlyProperty { _, _ ->
                    diskOptions
                        .map { it.disk }
                        .singleOrNull()
                        ?.apply {
                            check(exists()) { "Disk $this does no exist." }
                            check(isReadable()) { "Disk $this is not readable." }
//                            TODO test for GitHub workflows
//                            check(isWritable()) { "Disk $this is not writable." }
                        }
                        ?: error("Only one disk is supported but ${disks.size} where given: ${disks.joinToString(", ")}.")
                }
        }
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
