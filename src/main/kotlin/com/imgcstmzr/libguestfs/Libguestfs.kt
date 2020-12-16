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

fun OperatingSystemImage.resolveOnHost(pathInsideOfImage: Path) =
    with(SharedPath.Host) { resolve(pathInsideOfImage) }

fun OperatingSystemImage.resolveOnHost(pathInsideOfImage: String) =
    with(SharedPath.Host) { resolve(pathInsideOfImage) }

fun OperatingSystemImage.resolveOnDocker(pathInsideOfImage: Path) =
    with(SharedPath.Docker) { resolve(pathInsideOfImage) }

fun OperatingSystemImage.resolveOnDocker(pathInsideOfImage: String) =
    with(SharedPath.Docker) { resolve(pathInsideOfImage) }

fun OperatingSystemImage.resolveOnDisk(pathInsideOfImage: Path) =
    with(SharedPath.Disk) { resolve(pathInsideOfImage) }

fun OperatingSystemImage.resolveOnDisk(pathInsideOfImage: String) =
    with(SharedPath.Disk) { resolve(pathInsideOfImage) }






