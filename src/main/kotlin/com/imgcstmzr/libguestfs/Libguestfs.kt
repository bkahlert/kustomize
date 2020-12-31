@file:Suppress("SpellCheckingInspection")

package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishCommandLineBuilder.Companion.build
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeCommandLineBuilder.Companion.build
import com.imgcstmzr.runtime.OperatingSystemImage
import java.nio.file.Path

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
        fun of(osImage: OperatingSystemImage): Libguestfs =
            Libguestfs(osImage)

        fun OperatingSystemImage.libguestfs(): Libguestfs =
            Libguestfs(this)
    }
}

fun OperatingSystemImage.resolveOnHost(diskAbsolutePath: Path) =
    with(SharedPath.Host) { resolve(diskAbsolutePath) }

fun OperatingSystemImage.resolveOnHost(diskAbsolutePath: String) =
    with(SharedPath.Host) { resolve(diskAbsolutePath) }

fun OperatingSystemImage.resolveOnDocker(diskAbsolutePath: Path) =
    with(SharedPath.Docker) { resolve(diskAbsolutePath) }

fun OperatingSystemImage.resolveOnDocker(diskAbsolutePath: String) =
    with(SharedPath.Docker) { resolve(diskAbsolutePath) }

fun OperatingSystemImage.resolveOnDisk(diskAbsolutePath: Path) =
    with(SharedPath.Disk) { resolve(diskAbsolutePath) }

fun OperatingSystemImage.resolveOnDisk(diskAbsolutePath: String) =
    with(SharedPath.Disk) { resolve(diskAbsolutePath) }






