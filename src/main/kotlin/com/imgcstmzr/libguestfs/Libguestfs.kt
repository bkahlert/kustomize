@file:Suppress("SpellCheckingInspection")

package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.builder.buildList
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.runtime.OperatingSystemImage
import java.nio.file.Path

/**
 * Libguestfs integration
 *
 * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
 */
object Libguestfs {

    object Guestfish {
        fun commands(init: GuestfishCommandLine.GuestfishCommandsBuilder.() -> Unit) = init.buildList()
    }

    object VirtCustomize {
        fun customizations(init: VirtCustomizeCommandLine.VirtCustomizeCustomizationOptionsBuilder.() -> Unit) = init.buildList()
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






