package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.CopyIn
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.CopyOut
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.TarIn
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.ExitCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.RmCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.RmDirCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.TarOutCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.TouchCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.UmountAllCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.DiskOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.MountOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.ReadOnlyOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.ReadWriteOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.TraceOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.VerboseOption
import com.imgcstmzr.libguestfs.LibguestfsOption.Companion.relativize
import com.imgcstmzr.os.DiskDirectory
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemImage.Companion.mountRootForDisk
import koodies.builder.buildList
import koodies.collections.head
import koodies.collections.tail
import koodies.docker.DockerContainer
import koodies.docker.DockerExec
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.MountOptions
import koodies.docker.asContainerPath
import koodies.exec.Executable
import koodies.io.compress.TarArchiver.tar
import koodies.io.path.asPath
import koodies.io.path.pathString
import koodies.shell.HereDoc
import koodies.shell.ShellScript
import koodies.text.LineSeparators
import koodies.text.LineSeparators.size
import koodies.text.withPrefix
import koodies.text.withRandomSuffix
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.moveTo

@DslMarker
annotation class GuestfishDsl

/**
 * Guestfish is a shell and command-line tool for examining and modifying virtual machine filesystems.
 *
 * It uses [Libguestfs] and exposes all of the functionality of the [guestfs API](https://libguestfs.org/guestfs.3.html).
 */
class GuestfishCommandLine(
    val options: GuestfishOptions,
    val commands: List<GuestfishCommand>,
    val dockerOptions: Options = Options(
        name = DockerContainer.from(COMMAND.withRandomSuffix()),
        autoCleanup = true,
        mounts = MountOptions {
            mountRootForDisk(options.disk) mountAt "/shared"
            options.disk mountAt "/images/disk.img"
        },
        workingDirectory = "/shared".asContainerPath(),
        custom = buildList {
            if (options.filterIsInstance<TraceOption>().isNotEmpty()) {
                add("--env")
                add("LIBGUESTFS_TRACE=1")
            }
            if (options.filterIsInstance<VerboseOption>().isNotEmpty()) {
                add("--env")
                add("LIBGUESTFS_DEBUG=1")
            }
        }
    ),
) : Executable<DockerExec> by DockerRunCommandLine(
    LibguestfsImage,
    dockerOptions,
    ShellScript(when (size) {
        0 -> "No $COMMAND operations"
        1 -> "One $COMMAND operation"
        else -> "$size $COMMAND operations"
    }) {

        line(
            COMMAND,
            *options.map { (it as? DiskOption)?.let { DiskOption("/images/disk.img".asPath()) } ?: it }.flatten().toTypedArray(),
            HereDoc {
                commands.forEach { guestfishCommand ->
                    val relativizedCommand: LibguestfsOption = guestfishCommand.relativize(options.disk)
                    add(relativizedCommand.joinToString(" "))
                }

                if (commands.filterIsInstance<MountOption>().isNotEmpty() &&
                    commands.filterIsInstance<UmountAllCommand>().isEmpty()
                ) {
                    add(UmountAllCommand.joinToString(" "))
                }

                if (commands.filterIsInstance<ExitCommand>().isEmpty()) {
                    add(ExitCommand.joinToString(" "))
                }
            },
        )
    }
) {

    override fun toString(): String = toCommandLine().toString()

    companion object {
        private const val COMMAND = "guestfish"
    }

    data class GuestfishOptions(
        val readOnly: Boolean,
        val readWrite: Boolean,
        val verbose: Boolean,
        val trace: Boolean,
        val disks: List<Path>,
    ) : List<GuestfishOption> by (buildList {
        if (readOnly) add(ReadOnlyOption)
        if (readWrite) add(ReadWriteOption)
        disks.forEach { add(DiskOption(it)) }
        // inspector does not seem to mount `/boot`; therefore mount options are manually added
        add(MountOption("/dev/sda2".asPath(), LinuxRoot))
        add(MountOption("/dev/sda1".asPath(), LinuxRoot.boot))
        if (verbose) add(VerboseOption)
        if (trace) add(TraceOption)
    }) {
        constructor(
            vararg disks: Path,
            readOnly: Boolean = false,
            readWrite: Boolean = true,
            verbose: Boolean = false,
            trace: Boolean = false,
        ) : this(readOnly, readWrite, verbose, trace, disks.toList())

        val disk: Path = filterIsInstance<DiskOption>().map { it.disk }
            .also { disks -> check(disks.size == 1) { "The $COMMAND command must add exactly one disk. ${disks.size} found: ${disks.joinToString(", ")}." } }
            .single().apply {
                check(exists()) { "Disk $this does no exist." }
                check(isReadable()) { "Disk $this is not readable." }
                check(isWritable()) { "Disk $this is not writable." }
            }
    }


    open class GuestfishOption(name: String, arguments: List<String>) : LibguestfsOption(name, arguments) {

        /**
         * Use the this option to use guestfish safely if the disk image or virtual machine might be live.
         *
         * You may see strange or inconsistent results if running concurrently with other changes,
         * but with this option you won't risk disk corruption.
         *
         * The format of the disk image is auto-detected.
         */
        object ReadOnlyOption : GuestfishOption("--ro", emptyList())

        /**
         * At the time of writing, this version of guestfish has a [ReadWriteOption] which does nothing
         * (it is already the default).
         *
         * However it is **highly recommended** that you use this option to indicate that you need write access,
         * and prepare your scripts for the day when this option will be required for write access.
         */
        object ReadWriteOption : GuestfishOption("--rw", emptyList())

        /**
         * Add a block device or virtual machine image to the shell.
         *
         * The format of the disk image is auto-detected.
         */
        class DiskOption(override val disk: Path) : GuestfishOption("--add", listOf(disk.pathString)), LibguestfsOption.DiskOption

        /**
         * Using [virt-inspector](https://libguestfs.org/virt-inspector.1.html) code,
         * inspects the disks looking for an operating system and mounts filesystems
         * as they would be mounted on the real virtual machine.
         */
        object InspectorOption : GuestfishOption("--inspector", emptyList())

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
        class MountOption(val hostPath: Path, val mountPoint: DiskPath?, val options: List<String> = emptyList(), val fsType: String? = null) :
            GuestfishOption("--mount",
                listOf(StringBuilder(hostPath.pathString).apply {
                    mountPoint?.also {
                        append(":")
                        append(mountPoint.toString())

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

        /**
         * Enable very verbose messages.
         */
        object VerboseOption : GuestfishOption("--verbose", emptyList())

        /**
         * Echo each command before executing it.
         */
        object TraceOption : GuestfishOption("-x", emptyList())
    }


    /**
     * Commands supported by the [GuestfishCommandLine].
     */
    open class GuestfishCommand(name: String, arguments: List<String>) : LibguestfsOption(name, arguments) {
        constructor(name: String, vararg arguments: String) : this(name, arguments.toList())

        /**
         * Returns a copy of this command which runs on the (dockerized) host
         * instead of in the guest by preceding it with a `!`.
         *
         * Any line which starts with a `!` character is treated as a command sent to the local shell
         * (`/bin/sh` or whatever [system(3)](http://man.he.net/man3/system) uses).
         */
        operator fun not(): GuestfishCommand =
            GuestfishCommand("!$name", arguments)

        /**
         * By default, guestfish will ignore any errors when in interactive mode (ie. taking commands from a human over a tty),
         * and will exit on the first error in non-interactive mode (scripts, commands given on the command line).
         *
         * This method returns a copy of this command that will not cause guestfish to exit, even if this command returns an error.
         */
        operator fun unaryMinus(): GuestfishCommand =
            GuestfishCommand(name.withPrefix("-"), arguments)


        /**
         * This command packs the contents of [directory] and downloads it to the [archive].
         */
        class TarOutCommand(val directory: DiskDirectory = LinuxRoot, val archive: Path) : GuestfishCommand(
            "tar-out",
            directory.pathString,
            archive.pathString,
            "excludes:${directory / archive.fileName.pathString}"
        )

        /**
         * Touch acts like the [touch(1)](http://man.he.net/man1/touch) command.
         * It can be used to update the timestamps on a [file], or, if the file does not exist, to create a new zero-length file.
         *
         * This command only works on regular files, and will fail on other file types such as directories, symbolic links, block special etc.
         */
        class TouchCommand(val file: DiskPath) : GuestfishCommand(
            "touch",
            file.pathString,
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
        class RmDirCommand(val directory: DiskPath) : GuestfishCommand("rmdir", directory.pathString)

        /**
         * This unmounts all mounted filesystems.
         *
         * Some internal mounts are not unmounted by this call.
         */
        object UmountAllCommand : GuestfishCommand("umount-all")

        /**
         * This exits guestfish.
         */
        object ExitCommand : GuestfishCommand("exit")

        open class Composite(val guestfishCommands: List<GuestfishCommand>) :
            GuestfishCommand(
                guestfishCommands.head.name,
                buildList {
                    addAll(guestfishCommands.head.arguments)
                    add(LineSeparators.LF)
                    guestfishCommands.tail.forEach {
                        addAll(it)
                        add(LineSeparators.LF)
                    }
                }
            ) {

            override fun toString(): String = guestfishCommands.joinToString(LineSeparators.LF)

            /**
             * Copies [localFiles] or directories recursively into the disk image,
             * placing them in the corresponding locations (which must exist depending on [mkDir]).
             *
             * This guestfish meta-command turns into a sequence of [TarIn] and other commands as necessary.
             *
             * Multiple local files and directories can be specified. Wildcards cannot be used.
             */
            class CopyIn(val mkDir: Boolean, remoteDir: DiskPath, vararg val localFiles: Path) :
                Composite(listOfNotNull(
                    if (mkDir) -GuestfishCommand("mkdir-p", remoteDir.pathString) else null,
                    -GuestfishCommand(
                        "copy-in",
                        *localFiles.map { it.pathString }.toTypedArray(),
                        remoteDir.pathString,
                    ),
                ))

            /**
             * Copies [remoteFiles] or directories recursively out of the disk image,
             * placing them on the host disk in the corresponding directory (which must exist depending on [mkDir]) shared directory.
             *
             * This guestfish meta-command turns into a sequence of `download`, [TarOutCommand] and other commands as necessary.
             *
             * Multiple remote files and directories can be specified.
             */
            class CopyOut(val mkDir: Boolean, val directory: Path, val remoteFiles: DiskPath) :
                Composite(listOfNotNull(
                    if (mkDir) !GuestfishCommand("mkdir", "-p", directory.pathString) else null,
                    -GuestfishCommand("copy-out",
                        remoteFiles.pathString,
                        directory.pathString
                    ),
                ))

            /**
             * This command uploads the [archive] (must be accessible from the guest) and unpacks it into [directory].
             */
            class TarIn(deleteArchiveAfterwards: Boolean, val archive: Path, val directory: DiskPath) :
                Composite(listOfNotNull(
                    GuestfishCommand(
                        "tar-in",
                        archive.pathString,
                        directory.pathString,
                    ),
                    if (deleteArchiveAfterwards) !RmCommand(archive.pathString) else null,
                ))
        }
    }

    class GuestfishCommandsBuilder(private val osImage: OperatingSystemImage) {

        fun build(init: GuestfishCommandsContext.() -> Unit): List<GuestfishCommand> =
            mutableListOf<GuestfishCommand>().also { GuestfishCommandsContext(it).init() }

        @GuestfishDsl
        inner class GuestfishCommandsContext(private val guestfishCommands: MutableList<GuestfishCommand>) {

            /**
             * This creates a raw command that calls [name] and passes [arguments].
             */
            fun custom(name: String, vararg arguments: String) {
                guestfishCommands.add(GuestfishCommand(name, *arguments))
            }

            /**
             * This command copies local files or directories recursively into the disk image.
             */
            fun copyIn(mkDir: Boolean = true, remoteDir: (OperatingSystemImage) -> DiskPath) {
                guestfishCommands.add(remoteDir(osImage).run { CopyIn(mkDir, parentOrNull ?: this, osImage.hostPath(this)) })
            }

            /**
             * This command copies remote files or directories recursively out of the disk image.
             */
            fun copyOut(mkDir: Boolean = true, remoteFile: (OperatingSystemImage) -> DiskPath) {
                guestfishCommands.add(remoteFile(osImage).run { CopyOut(mkDir, osImage.hostPath(this).parent ?: osImage.hostPath(this), this) })
            }

            /**
             * This command uploads and unpacks the local host share to the guest VMs root `/`.
             */
            fun tarIn(diskPath: (OperatingSystemImage) -> DiskPath = { LinuxRoot }) {
                guestfishCommands.add(diskPath(osImage).run { TarIn(true, tar(osImage), this) })
            }

            /**
             * Creates a tar archive of `this` [DiskPath] and places the archive inside of this path.
             */
            private fun DiskPath.tar(osImage: OperatingSystemImage, archiveName: String = "archive.tar"): Path =
                osImage.hostPath(this).run { resolve(archiveName).also { target -> tar(parent / archiveName).moveTo(target) } }

            /**
             * * This command packs the guest VMs root `/` and downloads it to the local host share.
             */
            fun tarOut() {
                guestfishCommands.add(TarOutCommand(LinuxRoot, osImage.hostPath(LinuxRoot / "archive.tar")))
            }

            /**
             * Touch acts like the [touch(1)](http://man.he.net/man1/touch) command.
             * It can be used to update the timestamps on a file, or, if the file does not exist, to create a new zero-length file.
             *
             * This command only works on regular files, and will fail on other file types such as directories, symbolic links, block special etc.
             */
            fun touch(init: () -> DiskPath) {
                guestfishCommands.add(TouchCommand(init()))
            }

            /**
             * Remove the single [file].
             *
             * If [force] is specified and the file doesn't exist, that error is ignored. (Other errors, eg. I/O errors or bad paths, are not ignored)
             *
             * This call cannot remove directories. Use [RmDirCommand] to remove an empty directory, or set [recursive] to remove directories recursively.
             */
            fun rm(force: Boolean = false, recursive: Boolean = false, file: () -> DiskPath) {
                guestfishCommands.add(RmCommand(file().pathString, force, recursive))
            }

            /**
             * Remove the single [directory].
             */
            fun rmDir(directory: () -> DiskPath) {
                guestfishCommands.add(RmDirCommand(directory()))
            }

            /**
             * This unmounts all mounted filesystems.
             *
             * Some internal mounts are not unmounted by this call.
             */
            fun umountAll() {
                guestfishCommands.add(UmountAllCommand)
            }

            /**
             * This exits guestfish.
             */
            fun exit() {
                guestfishCommands.add(ExitCommand)
            }
        }
    }
}
