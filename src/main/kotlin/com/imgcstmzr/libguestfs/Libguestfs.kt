@file:Suppress("SpellCheckingInspection")

package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.GuestfishCommandLineContext
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.VirtCustomizeCommandLineContext
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.builder.Init
import koodies.io.file.resolveBetweenFileSystems
import koodies.io.path.asPath
import koodies.io.path.asString
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.relativeTo

/**
 * Libguestfs integration
 *
 * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
 */
class Libguestfs(private val osImage: OperatingSystemImage) {

    fun guestfish(init: Init<GuestfishCommandLineContext>): GuestfishCommandLine = GuestfishCommandLine.build(osImage, init)

    fun virtCustomize(init: Init<VirtCustomizeCommandLineContext>): VirtCustomizeCommandLine = VirtCustomizeCommandLine.build(osImage, init)

    companion object {
        private const val DEFAULT_CONTAINER_MOUNT_POINT_ON_HOST = "shared"

        /**
         * Returns the directory used to share (i.e. for copying-in and -out)
         * files between this host and the [OperatingSystem] contained in the
         * given [disk] file.
         *
         * Example: For [disk] `$HOME/.imgcstmzr/project/os.img` the mount root
         * would be `$HOME/.imgcstmzr/project/shared`.
         */
        fun mountRootForDisk(disk: HostPath): HostPath =
            disk.resolveSibling(DEFAULT_CONTAINER_MOUNT_POINT_ON_HOST).createDirectories()

        /**
         * Given the [diskPath] that locates a file inside `this` [OperatingSystemImage]
         * this method returns the corresponding mapped location on the host.
         *
         * Example: For [diskPath] `/var/file` the mapped host path would be
         * `$HOME/.imgcstmzr/project/shared/var/file`.
         */
        fun OperatingSystemImage.hostPath(diskPath: DiskPath): HostPath =
            diskPath.hostPath(mountRootForDisk(file))

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
    val parent: DiskPath? get() = asPath().parent?.let { DiskPath(it.asString()) }
    val requiredParent: DiskPath get() = parent ?: error("Parent required but $path has none.")
    fun resolve(path: String): DiskPath = DiskPath(asPath().resolve(path).asString())
    fun hostPath(mountRoot: HostPath): Path {
        val rootRelative = asPath().let { diskPath -> diskPath.takeIf { !it.isAbsolute } ?: diskPath.relativeTo(ROOT) }
        return mountRoot.resolveBetweenFileSystems(rootRelative)
    }

    fun asString(): String = asPath().asString()
    override fun toString(): String = asString()

    companion object {
        private val ROOT = "/".asPath()
    }
}
