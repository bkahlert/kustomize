package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.GuestfishCommandLine.Companion.GuestfishCommandLineContext
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.CopyIn
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.CopyOut
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.TarIn
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.ExitCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.RmCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.RmDirCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.TarOutCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.TouchCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.UmountAllCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder.GuestfishCommandsContext
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.DiskOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.InspectorOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.MountOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.ReadOnlyOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.ReadWriteOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.TraceOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOption.VerboseOption
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishOptionsBuilder.GuestfishOptionsContext
import com.imgcstmzr.libguestfs.LibguestfsOption.Companion.relativize
import com.imgcstmzr.os.DiskDirectory
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemImage.Companion.mountRootForDisk
import koodies.builder.BooleanBuilder
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.build
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.collections.head
import koodies.collections.tail
import koodies.docker.DockerExec
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.asContainerPath
import koodies.exec.Executable
import koodies.io.compress.TarArchiver.tar
import koodies.io.path.asPath
import koodies.io.path.pathString
import koodies.kaomoji.Kaomoji
import koodies.kaomoji.Kaomoji.Companion.Fish
import koodies.math.ceilDiv
import koodies.math.floorDiv
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
    val options: List<GuestfishOption>,
    val commands: List<GuestfishCommand>,
    val disk: Path = options.filterIsInstance<DiskOption>().map { it.disk }
        .also { disks -> check(disks.size == 1) { "The $COMMAND command must add exactly one disk. ${disks.size} found: ${disks.joinToString(", ")}." } }
        .single().apply {
            check(exists()) { "Disk $this does no exist." }
            check(isReadable()) { "Disk $this is not readable." }
            check(isWritable()) { "Disk $this is not writable." }
        },
    val dockerOptions: Options = Options {
        name { COMMAND.withRandomSuffix() }
        autoCleanup { on }
        mounts {
            mountRootForDisk(disk) mountAt "/shared"
            disk mountAt "/images/disk.img"
        }
        workingDirectory { "/shared".asContainerPath() }
        custom {
            if (options.filterIsInstance<TraceOption>().isNotEmpty()) {
                add("--env")
                add("LIBGUESTFS_TRACE=1")
            }
            if (options.filterIsInstance<VerboseOption>().isNotEmpty()) {
                add("--env")
                add("LIBGUESTFS_DEBUG=1")
            }
        }
    },
) : Executable<DockerExec> by DockerRunCommandLine(
    LibguestfsImage,
    dockerOptions,
    ShellScript {

        val nonDiskOptions: List<GuestfishOption> = options.filter { it !is DiskOption } +
            listOf(
                // inspector { on } // does not mount /boot
                MountOption("/dev/sda2".asPath(), LinuxRoot),
                MountOption("/dev/sda1".asPath(), LinuxRoot.boot),
            )

        line(
            COMMAND,
            *ReadWriteOption.toTypedArray(),
            *DiskOption("/images/disk.img".asPath()).toTypedArray(),
            *nonDiskOptions.flatten().toTypedArray(),
            HereDoc {
                commands.forEach { guestfishCommand ->
                    val relativizedCommand: LibguestfsOption = guestfishCommand.relativize(disk)
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

    override val name: CharSequence = when (size) {
        0 -> "No $COMMAND operations ${Kaomoji.random().fishing(Kaomoji.EMPTY)}"
        1 -> "One $COMMAND operation ${Kaomoji.random().fishing(Fish.`❮°«⠶＞˝`)}"
        else -> "$size $COMMAND operations ${
            Kaomoji.random().fishing(Kaomoji("❮", "°", "⠶".repeat(size floorDiv 3) + "«", "⠶".repeat(size * 2 ceilDiv 3), "＞", "˝"))
        }"
    }

    override fun toString(): String = toCommandLine().toString()

    companion object : BuilderTemplate<GuestfishCommandLineContext, (OperatingSystemImage) -> GuestfishCommandLine>() {

        private const val COMMAND = "guestfish"

        @GuestfishDsl
        class GuestfishCommandLineContext(override val captures: CapturesMap) : CapturingContext() {

            val options by GuestfishOptionsBuilder default { emptyList<(OperatingSystemImage) -> GuestfishOption>() }
            val commands by GuestfishCommandsBuilder default { emptyList<(OperatingSystemImage) -> GuestfishCommand>() }
        }

        override fun BuildContext.build(): (OperatingSystemImage) -> GuestfishCommandLine =
            ::GuestfishCommandLineContext {
                { osImage: OperatingSystemImage ->
                    val options: List<(OperatingSystemImage) -> GuestfishOption> = ::options.eval()
                    val commands: List<(OperatingSystemImage) -> GuestfishCommand> = ::commands.eval()
                    GuestfishCommandLine(options.map { it(osImage) }, commands.map { it(osImage) })
                }
            }

        fun build(osImage: OperatingSystemImage, init: Init<GuestfishCommandLineContext>) = build(init)(osImage)
        operator fun invoke(osImage: OperatingSystemImage, init: Init<GuestfishCommandLineContext>) =
            build(init)(osImage)
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


    object GuestfishOptionsBuilder : BuilderTemplate<GuestfishOptionsContext, List<(OperatingSystemImage) -> GuestfishOption>>() {

        @GuestfishDsl
        class GuestfishOptionsContext(override val captures: CapturesMap) : CapturingContext() {

            val option by function<(OperatingSystemImage) -> GuestfishOption>()

            /**
             * Use the this option to use guestfish safely if the disk image or virtual machine might be live.
             */
            val readOnly by BooleanBuilder.OnOff then {
                if (it) option { ReadOnlyOption }
            }

            /**
             * Use the this option to explicitly allow write access. In future guestfish releases
             * [readOnly] might become the default setting.
             */
            val readWrite by BooleanBuilder.OnOff then {
                if (it) option { ReadWriteOption }
            }

            /**
             * Add file which should be a disk image from a virtual machine.
             *
             * The format of the disk image is auto-detected.
             */
            fun disk(path: (OperatingSystemImage) -> Path) {
                option { DiskOption(path(it)) }
            }

            /**
             * Add file which should be a disk image from a virtual machine.
             *
             * The format of the disk image is auto-detected.
             */
            val inspector by BooleanBuilder.OnOff then {
                if (it) option { InspectorOption() }
            }

            /**
             * Mount the named partition or logical volume on the given mount point,
             * e.g. `Path.of("/dev/sda1") to Path.of("/")`
             */
            fun mount(init: (OperatingSystemImage) -> Pair<Path, DiskPath>) {
                option { init(it).run { MountOption(first, second) } }
            }

            /**
             * Enable very verbose messages.
             */
            val verbose by BooleanBuilder.OnOff then {
                if (it) option { VerboseOption }
            }

            /**
             * Echo each command before executing it.
             */
            val trace by BooleanBuilder.OnOff then {
                if (it) option { TraceOption }
            }
        }

        override fun BuildContext.build() = ::GuestfishOptionsContext { ::option.evalAll() }
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
                koodies.builder.buildList {
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

    object GuestfishCommandsBuilder : BuilderTemplate<GuestfishCommandsContext, List<(OperatingSystemImage) -> GuestfishCommand>>() {

        @GuestfishDsl
        class GuestfishCommandsContext(override val captures: CapturesMap) : CapturingContext() {

            val command by function<(OperatingSystemImage) -> GuestfishCommand>()

            /**
             * This creates a raw command that calls [name] and passes [arguments].
             */
            fun custom(name: String, vararg arguments: String) {
                command { GuestfishCommand(name, *arguments) }
            }

            /**
             * This command copies local files or directories recursively into the disk image.
             */
            fun copyIn(mkDir: Boolean = true, remoteDir: (OperatingSystemImage) -> DiskPath) {
                command { remoteDir(it).run { CopyIn(mkDir, parentOrNull ?: this, it.hostPath(this)) } }
            }

            /**
             * This command copies remote files or directories recursively out of the disk image.
             */
            fun copyOut(mkDir: Boolean = true, remoteFile: (OperatingSystemImage) -> DiskPath) {
                command { remoteFile(it).run { CopyOut(mkDir, it.hostPath(this).parent ?: it.hostPath(this), this) } }
            }

            /**
             * This command uploads and unpacks the local host share to the guest VMs root `/`.
             */
            fun tarIn(diskPath: (OperatingSystemImage) -> DiskPath = { LinuxRoot }) {
                command { diskPath(it).run { TarIn(true, tar(it), this) } }
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
                command { TarOutCommand(LinuxRoot, it.hostPath(LinuxRoot / "archive.tar")) }
            }

            /**
             * Touch acts like the [touch(1)](http://man.he.net/man1/touch) command.
             * It can be used to update the timestamps on a file, or, if the file does not exist, to create a new zero-length file.
             *
             * This command only works on regular files, and will fail on other file types such as directories, symbolic links, block special etc.
             */
            fun touch(init: () -> DiskPath) {
                command { TouchCommand(init()) }
            }

            /**
             * Remove the single [file].
             *
             * If [force] is specified and the file doesn't exist, that error is ignored. (Other errors, eg. I/O errors or bad paths, are not ignored)
             *
             * This call cannot remove directories. Use [RmDirCommand] to remove an empty directory, or set [recursive] to remove directories recursively.
             */
            fun rm(force: Boolean = false, recursive: Boolean = false, file: () -> DiskPath) {
                command { RmCommand(file().pathString, force, recursive) }
            }

            /**
             * Remove the single [directory].
             */
            fun rmDir(directory: () -> DiskPath) {
                command { RmDirCommand(directory()) }
            }

            /**
             * This unmounts all mounted filesystems.
             *
             * Some internal mounts are not unmounted by this call.
             */
            fun umountAll() {
                command { UmountAllCommand }
            }

            /**
             * This exits guestfish.
             */
            fun exit() {
                command { ExitCommand }
            }
        }

        override fun BuildContext.build() = ::GuestfishCommandsContext{ ::command.evalAll() }
    }
}
