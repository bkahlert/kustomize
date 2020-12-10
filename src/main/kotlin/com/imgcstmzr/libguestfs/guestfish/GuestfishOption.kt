package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.nio.file.serialized
import com.imgcstmzr.libguestfs.Option
import java.nio.file.Path

sealed class GuestfishOption(name: String, arguments: List<String>) : Option(name, arguments) {

    /**
     * Add a block device or virtual machine image to the shell.
     *
     * The format of the disk image is auto-detected.
     */
    class DiskOption(override val disk: Path) : GuestfishOption("--add", listOf(disk.serialized)), com.imgcstmzr.libguestfs.DiskOption

    /**
     * Using [virt-inspector](https://libguestfs.org/virt-inspector.1.html) code,
     * inspects the disks looking for an operating system and mounts filesystems
     * as they would be mounted on the real virtual machine.
     */
    class InspectorOption : GuestfishOption("--inspector", emptyList())
}
