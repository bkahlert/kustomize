package com.imgcstmzr.libguestfs.guestfish

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import koodies.io.path.asString
import koodies.text.LineSeparators
import koodies.text.withPrefix

/**
 * Commands supported by the [GuestfishCommandLine].
 */
open class GuestfishCommand(
    open val name: String, private vararg val arguments: String,

    /**
     * Whether this command is not part of the [guestfs API](https://libguestfs.org/guestfs.3.html).
     */
    @Suppress("UNUSED_PARAMETER") private val convenience: Boolean = false,
) : List<String> by listOf(name) + arguments {

    /**
     * Returns a copy of this command which runs on the (dockerized) host
     * instead of in the guest by preceding it with a `!`.
     *
     * Any line which starts with a `!` character is treated as a command sent to the local shell
     * (`/bin/sh` or whatever [system(3)](http://man.he.net/man3/system) uses).
     */
    operator fun not(): GuestfishCommand =
        GuestfishCommand("!$name", convenience = convenience, arguments = arguments)

    /**
     * By default, guestfish will ignore any errors when in interactive mode (ie. taking commands from a human over a tty),
     * and will exit on the first error in non-interactive mode (scripts, commands given on the command line).
     *
     * This method returns a copy of this command that will not cause guestfish to exit, even if this command returns an error.
     */
    operator fun unaryMinus(): GuestfishCommand =
        GuestfishCommand(name.withPrefix("-"), convenience = convenience, arguments = arguments)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GuestfishCommand

        if (name != other.name) return false
        if (!arguments.contentEquals(other.arguments)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + arguments.contentHashCode()
        return result
    }

    override fun toString(): String = "${this::class.simpleName}(${this.joinToString(" ")})"
}

open class GuestfishCompositeCommand(val guestfishCommands: List<GuestfishCommand>) :
    GuestfishCommand(guestfishCommands[0].first(), *guestfishCommands.toList()
        .map { it + LineSeparators.LF }
        .flatten().drop(1).toTypedArray(), convenience = true) {

    override fun toString(): String = guestfishCommands.joinToString(LineSeparators.LF)
}


/**
 * Copies [localFiles] or directories recursively into the disk image,
 * placing them in the corresponding locations (which must exist depending on [mkDir]).
 *
 * This guestfish meta-command turns into a sequence of [TarInCommand] and other commands as necessary.
 *
 * Multiple local files and directories can be specified. Wildcards cannot be used.
 */
class CopyInCommand(val mkDir: Boolean, remoteDir: DiskPath, vararg val localFiles: HostPath) :
    GuestfishCompositeCommand(listOfNotNull(
        if (mkDir) -GuestfishCommand("mkdir-p", remoteDir.toString()) else null,
        -GuestfishCommand("copy-in",
            *localFiles.map { it.asString() }.toTypedArray(),
            remoteDir.toString(),
            convenience = true),
    )) {
}

/**
 * Copies [remoteFiles] or directories recursively out of the disk image,
 * placing them on the host disk in the corresponding directory (which must exist depending on [mkDir]) shared directory.
 *
 * This guestfish meta-command turns into a sequence of `download`, [TarOutCommand] and other commands as necessary.
 *
 * Multiple remote files and directories can be specified.
 */
class CopyOutCommand(val mkDir: Boolean, val directory: HostPath, val remoteFiles: DiskPath) :
    GuestfishCompositeCommand(listOfNotNull(
        if (mkDir) !GuestfishCommand("mkdir", "-p", directory.asString()) else null,
        -GuestfishCommand("copy-out",
            remoteFiles.asString(),
            directory.asString(),
            convenience = true),
    ))

/**
 * This command uploads the [archive] (must be accessible from the guest) and unpacks it into [directory].
 */
class TarInCommand(deleteArchiveAfterwards: Boolean, val archive: HostPath, val directory: DiskPath) :
    GuestfishCompositeCommand(listOfNotNull(
        GuestfishCommand(
            "tar-in",
            archive.asString(),
            directory.toString()
        ),
        if (deleteArchiveAfterwards) !RmCommand(archive.asString()) else null,
    ))

/**
 * This command packs the contents of [directory] and downloads it to the [archive].
 */
class TarOutCommand(val directory: DiskPath = DiskPath("/"), val archive: HostPath) : GuestfishCommand(
    "tar-out",
    directory.toString(),
    archive.asString(),
    "excludes:${directory.resolve(archive.fileName.asString())}"
)


/**
 * Touch acts like the [touch(1)](http://man.he.net/man1/touch) command.
 * It can be used to update the timestamps on a [file], or, if the file does not exist, to create a new zero-length file.
 *
 * This command only works on regular files, and will fail on other file types such as directories, symbolic links, block special etc.
 */
class TouchCommand(val file: DiskPath) : GuestfishCommand(
    "touch",
    file.toString(),
)


/**
 * Remove the single [file].
 *
 * If [force] is specified and the file doesn't exist, that error is ignored. (Other errors, eg. I/O errors or bad paths, are not ignored)
 *
 * This call cannot remove directories. Use [RmDirCommand] to remove an empty directory, or set [recursive] to remove directories recursively.
 */
class RmCommand(val file: String, val force: Boolean = false, val recursive: Boolean = false) : GuestfishCommand("rm" + when (force to recursive) {
    true to false -> "-f"
    true to true -> "-rf"
    else -> ""
}, file)

/**
 * Remove the single [directory].
 */
class RmDirCommand(val directory: DiskPath) : GuestfishCommand("rmdir", directory.toString())

/**
 * This unmounts all mounted filesystems.
 *
 * Some internal mounts are not unmounted by this call.
 */
class UmountAllCommand : GuestfishCommand("umount-all", convenience = true)

/**
 * This exits guestfish.
 */
class ExitCommand : GuestfishCommand("exit", convenience = true)
