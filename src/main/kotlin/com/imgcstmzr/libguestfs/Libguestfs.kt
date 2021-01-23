@file:Suppress("SpellCheckingInspection")

package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishCommandLineBuilder.Companion.build
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeCommandLineBuilder.Companion.build
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.io.file.resolveBetweenFileSystems
import koodies.io.path.asPath
import koodies.io.path.asString
import java.nio.file.Path
import kotlin.io.path.relativeTo

/**
 * Libguestfs integration
 *
 * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
 */
class Libguestfs(private val osImage: OperatingSystemImage) {

    fun guestfish(init: GuestfishCommandLine.GuestfishCommandLineBuilder.() -> Unit): GuestfishCommandLine =
        init.build(osImage)

    fun virtCustomize(init: VirtCustomizeCommandLine.VirtCustomizeCommandLineBuilder.() -> Unit): VirtCustomizeCommandLine =
        init.build(osImage)

    companion object {
        private const val DEFAULT_CONTAINER_MOUNT_POINT_ON_HOST = "shared"

        fun mountRootForDisk(disk: HostPath): HostPath =
            disk.resolveSibling(DEFAULT_CONTAINER_MOUNT_POINT_ON_HOST)

        fun OperatingSystemImage.hostPath(diskPath: DiskPath): HostPath {
            val mountRoot: Path = mountRootForDisk(file)
            return diskPath.hostPath(mountRoot)
        }

        fun of(osImage: OperatingSystemImage): Libguestfs =
            Libguestfs(osImage)

        fun OperatingSystemImage.libguestfs(): Libguestfs =
            Libguestfs(this)
    }
}

typealias HostPath = Path

inline class DiskPath(val path: String) {
    val isAbsolute: Boolean get() = path.asPath().isAbsolute
    fun asPath(): Path = path.asPath()
    val fileName: Path get() = asPath().fileName
    val parent: DiskPath get() = DiskPath(asPath().parent.asString())
    fun resolve(path: String): DiskPath = DiskPath(asPath().resolve(path).asString())
    fun hostPath(mountRoot: HostPath): Path {
        val rootRelative = asPath().let { diskPath -> diskPath.takeIf { !it.isAbsolute } ?: diskPath.relativeTo(ROOT) }
        return mountRoot.resolveBetweenFileSystems(rootRelative)
    }

    override fun toString(): String = asPath().asString()

    companion object {
        private val ROOT = "/".asPath()
    }
}
