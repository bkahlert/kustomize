package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.nio.file.serialized
import com.imgcstmzr.libguestfs.Option
import java.nio.file.Path

sealed class GuestfishOption(name: String, arguments: List<String>) : Option(name, arguments) {

    /**
     * Not recommend but yet nice to have option to pass options to guestfish that
     * have no corresponding wrapper.
     */
    class Generic(vararg args: String) : GuestfishOption(args[0], args.drop(1))

    /**
     * Use the this option to use guestfish safely if the disk image or virtual machine might be live.
     *
     * You may see strange or inconsistent results if running concurrently with other changes,
     * but with this option you won't risk disk corruption.
     *
     * The format of the disk image is auto-detected.
     */
    class ReadOnlyOption() : GuestfishOption("--ro", emptyList())

    /**
     * At the time of writing, this version of guestfish has a [ReadWriteOption] which does nothing
     * (it is already the default).
     *
     * However it is **highly recommended** that you use this option to indicate that you need write access,
     * and prepare your scripts for the day when this option will be required for write access.
     */
    class ReadWriteOption() : GuestfishOption("--rw", emptyList())

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

    /**
     * Mount the named partition or logical volume on the given [mountPoint].
     *
     * If the mountpoint is omitted, it defaults to `/`.
     *
     * You have to mount something on `/` before most commands will work.
     *
     * If any [MountOption] is given, the guest is automatically launched.
     *
     * If you don’t know what filesystems a disk image contains, you can either run guestfish without this option,
     * then list the partitions, filesystems and LVs available (see [ListPartitionsOption], [ListFileSystemsOptions] and `lvs` commands),
     * or you can use the [virt-filesystems(1)](https://libguestfs.org/virt-filesystems.1.html) program.
     *
     * The third (and rarely used) part of the mount parameter is the list of mount [options]
     * used to mount the underlying filesystem.
     * If this is not given, then the mount options are either the empty string or ro (the latter if the [ReadOnlyOption] flag is used).
     *
     * By specifying the mount options, you override this default choice.
     * Probably the only time you would use this is to enable ACLs and/or extended attributes if the filesystem can support them
     * using `acl,user_xattr`
     *
     * The [fsType] parameter is the filesystem driver to use, such as `ext3` or `ntfs`.
     * This is rarely needed, but can be useful if multiple drivers are valid for a filesystem (eg: `ext2` and `ext3`),
     * or if libguestfs misidentifies a filesystem.
     */
    class MountOption(val dev: Path, val mountPoint: Path?, val options: List<String> = emptyList(), val fsType: String? = null) :
        GuestfishOption("--mount",
            listOf(StringBuilder(dev.serialized).apply {
                mountPoint?.also {
                    append(":")
                    append(mountPoint.serialized)

                    options.takeIf { it.isNotEmpty() }?.also {
                        append(":")
                        options.joinTo(this, ",")

                        fsType?.also {
                            append(":")
                            append(it)
                        }
                    }
                }
            }.toString()))

}
