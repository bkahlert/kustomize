package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.string.withPrefix
import com.bkahlert.koodies.string.withoutPrefix
import java.nio.file.Path

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
        GuestfishCommand("!$name", arguments = arguments, convenience = convenience)

    /**
     * By default, guestfish will ignore any errors when in interactive mode (ie. taking commands from a human over a tty),
     * and will exit on the first error in non-interactive mode (scripts, commands given on the command line).
     *
     * This method returns a copy of this command that will not cause guestfish to exit, even if this command returns an error.
     */
    operator fun unaryMinus(): GuestfishCommand =
        GuestfishCommand(name.withPrefix("-"), arguments = arguments, convenience = convenience)

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

    override fun toString(): String = "${this::class.simpleName}(name='$name', arguments=${arguments.joinToString(",")})"
}


/**
 * Copies [localFiles] or directories recursively into the disk image,
 * placing them in the corresponding locations (which must exist).
 *
 * This guestfish meta-command turns into a sequence of [TarInCommand] and other commands as necessary.
 *
 * Multiple local files and directories can be specified. Wildcards cannot be used.
 */
class CopyInCommand private constructor(val remoteDir: Path, vararg val localFiles: Path) :
    GuestfishCommand("copy-in",
        *localFiles.map { it.serialized }.toTypedArray(),
        remoteDir.serialized,
        convenience = true) {
    constructor(remoteDir: Path) : this(remoteDir.parent, Path.of("/shared").resolve(remoteDir.serialized.withoutPrefix("/")))
}

/**
 * Copies [remoteFiles] or directories recursively out of the disk image,
 * placing them on the host disk in the shared directory.
 *
 * This guestfish meta-command turns into a sequence of `download`, [TarOutCommand] and other commands as necessary.
 *
 * Multiple remote files and directories can be specified.
 */
class CopyOutCommand private constructor(vararg val remoteFiles: Path, val directory: Path) :
    GuestfishCommand("copy-out", *remoteFiles.map { it.serialized }.toTypedArray(), directory.serialized, convenience = true) {
    constructor(remoteFile: Path) : this(remoteFile, directory =
    Path.of("/shared").resolve(remoteFile.serialized.withoutPrefix("/")).parent)
}

/**
 * This command uploads the file `archive.tar` next to the disk file and unpacks it into [directory].
 */
class TarInCommand(val directory: Path = Path.of("/")) : GuestfishCommand("tar-in", "../archive.tar", directory.serialized)

/**
 * This command packs the contents of [directory] and downloads it next to the disk file in a file with name `archive.tar`.
 */
class TarOutCommand(val directory: Path = Path.of("/")) : GuestfishCommand("tar-out", directory.serialized, "../archive.tar")

/**
 * This unmounts all mounted filesystems.
 *
 * Some internal mounts are not unmounted by this call.
 */
class UmountAllCommand() : GuestfishCommand("umount-all", convenience = true)

/**
 * This exits guestfish.
 */
class ExitCommand() : GuestfishCommand("exit", convenience = true)
