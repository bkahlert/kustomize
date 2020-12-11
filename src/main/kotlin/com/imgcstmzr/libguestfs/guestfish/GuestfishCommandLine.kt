package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.builder.ListBuilderInit
import com.bkahlert.koodies.builder.OnOffBuilderInit
import com.bkahlert.koodies.builder.buildListTo
import com.bkahlert.koodies.builder.buildTo
import com.bkahlert.koodies.docker.DockerRunAdaptable
import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.string.withoutPrefix
import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.libguestfs.Option
import com.imgcstmzr.libguestfs.SharedPath
import com.imgcstmzr.libguestfs.docker.GuestfishDockerAdaptable
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishCommandLineBuilder.Companion.build
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.logging
import java.nio.file.Path
import kotlin.io.path.moveTo

@DslMarker
annotation class GuestfishDsl

/**
 * Guestfish is a shell and command-line tool for examining and modifying virtual machine filesystems.
 *
 * It uses [Libguestfs] and exposes all of the functionality of the [guestfs API](https://libguestfs.org/guestfs.3.html).
 */
class GuestfishCommandLine(val options: List<Option>, val guestfishCommands: List<GuestfishCommand>) :
    DockerRunAdaptable by GuestfishDockerAdaptable(options, guestfishCommands),
    LibguestfsCommandLine(
        command = COMMAND,
        arguments = options.flatten() +
            guestfishCommands.toMutableList().let {
                if (UmountAllCommand() !in it) it += UmountAllCommand()
                if (ExitCommand() !in it) it += ExitCommand()
                it.flatten()
            }) {

    override fun executionCaption() = "Running ${guestfishCommands.size} guestfish operations... ${Kaomojis.fishing()}"

    companion object {
        const val COMMAND = "guestfish"

        @GuestfishDsl
        fun build(init: GuestfishCommandLineBuilder.() -> Unit) = init.build()

        @GuestfishDsl
        fun RenderingLogger.fishGuest(operatingSystemImage: OperatingSystemImage, init: GuestfishCommandsBuilder.() -> Unit): Int =
            build {
                options {
                    generic("--rw")
                    disks { +operatingSystemImage.file }
//                    inspector { on } // does not mount /boot
                    generic("--mount", "/dev/sda2:/")
                    generic("--mount", "/dev/sda1:/boot")
                }
                commands(init)
            }.execute(this)

        fun OperatingSystemImage.copyOut(path: String): Path {
            logging("copying out $path") {
                fishGuest(this@copyOut) {
                    copyOut(path.toPath())
                }
            }
            return resolveOnHost(path)
        }
    }

    /**
     * Allows modifying configuration files.
     *
     * [If you don't want to use Augeas (you fool!)](https://libguestfs.org/guestfs.3.html#configuration-files)
     * then you can use [TarInCommand] resp. [TarOutCommand] to download the file and modify it at your liking.
     *
     * @see <a href="http://augeas.net/">Augeas - a configuration API</a>
     */
    val augeas = true // TODO

    @GuestfishDsl
    class GuestfishCommandLineBuilder(
        private val options: MutableList<GuestfishOption> = mutableListOf(),
        private val commands: MutableList<GuestfishCommand> = mutableListOf(),
    ) {
        companion object {
            /**
             * Using `this` [GuestfishCommandLineBuilder] builds a list of [Option].
             */
            @GuestfishDsl fun (GuestfishCommandLineBuilder.() -> Unit).build(): GuestfishCommandLine =
                GuestfishCommandLineBuilder().apply(this).let { GuestfishCommandLine(it.options, it.commands) }
        }

        @GuestfishDsl
        fun options(init: GuestfishOptionsBuilder.() -> Unit) = init.buildListTo(options)

        @GuestfishDsl
        fun commands(init: GuestfishCommandsBuilder.() -> Unit) = init.buildListTo(commands)
    }

    @GuestfishDsl
    class GuestfishOptionsBuilder : ListBuilder<GuestfishOption>() {

        @GuestfishDsl
        fun generic(vararg parts: String) =
            GuestfishOption.Generic(*parts).also { list.add(it) }

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        @GuestfishDsl
        fun disks(init: ListBuilderInit<Path>) =
            init.buildListTo(list) { GuestfishOption.DiskOption(this) }

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        @GuestfishDsl
        fun inspector(init: OnOffBuilderInit) =
            init.buildTo(list) { GuestfishOption.InspectorOption() }
    }

    @GuestfishDsl
    class GuestfishCommandsBuilder : ListBuilder<GuestfishCommand>() {

        /**
         * All commands passed here will be treated as commands sent to the local shell
         * (`/bin/sh` or whatever [system(3)](http://man.he.net/man3/system) uses).
         */
        @GuestfishDsl fun runLocally(init: GuestfishCommandsBuilder.() -> Unit): List<GuestfishCommand> =
            init.buildListTo(list) { !this }

        /**
         * All command passed here will not cause guestfish to exit, even if any of these commands returns an error.
         */
        @GuestfishDsl fun ignoreErrors(init: GuestfishCommandsBuilder.() -> Unit): List<GuestfishCommand> =
            init.buildListTo(list) { -this }

        /**
         * This creates a raw command that calls [name] and passes [arguments].
         */
        @GuestfishDsl fun command(name: String, vararg arguments: String): GuestfishCommand =
            GuestfishCommand(name, *arguments).also { list.add(it) }

        /**
         * This command copies local files or directories recursively into the disk image.
         */
        @GuestfishDsl fun copyIn(remoteDir: Path, mkDir: Boolean = true): CopyInCommand =
            CopyInCommand(
                mkDir = mkDir,
                remoteDir = remoteDir.parent,
                Path.of("/shared").resolve(remoteDir.serialized.withoutPrefix("/"))
            ).also { list.add(it) }

        /**
         * This command copies remote files or directories recursively out of the disk image.
         */
        @GuestfishDsl fun copyOut(remoteFile: Path, mkDir: Boolean = true): CopyOutCommand =
            CopyOutCommand(
                remoteFile,
                mkDir = mkDir,
                directory = Path.of("/shared").resolve(remoteFile.serialized.withoutPrefix("/")).parent
            ).also { list.add(it) }

        /**
         * This command uploads and unpacks the local [SharedPath.Host] directory to the [SharedPath.Disk] root directory.
         */
        @GuestfishDsl fun tarIn(osImage: OperatingSystemImage): TarInCommand {
            val rootDirectory = osImage.resolveOnHost("")
            val archiveOutsideRootDirectory = rootDirectory.tar(rootDirectory.parent.resolve("archive.tar"))
            archiveOutsideRootDirectory.moveTo(rootDirectory.resolve("archive.tar"))
            return TarInCommand(
                archive = SharedPath.Docker.resolveRoot(osImage).resolve("archive.tar"),
                directory = SharedPath.Disk.resolveRoot(osImage),
                deleteArchiveAfterwards = true,
            ).also { list.add(it) }
        }

        /**
         * * This command packs the root directory and downloads it to the [SharedPath.Host]'s shared directory.
         */
        @GuestfishDsl fun tarOut(osImage: OperatingSystemImage) =
            TarOutCommand(
                directory = SharedPath.Disk.resolveRoot(osImage),
                archive = SharedPath.Docker.resolveRoot(osImage).resolve("archive.tar"),
            ).also { list.add(it) }

        /**
         * Touch acts like the [touch(1)](http://man.he.net/man1/touch) command.
         * It can be used to update the timestamps on a [file], or, if the file does not exist, to create a new zero-length file.
         *
         * This command only works on regular files, and will fail on other file types such as directories, symbolic links, block special etc.
         */
        @GuestfishDsl fun touch(file: Path) =
            TouchCommand(file).also { list.add(it) }

        /**
         * Remove the single [file].
         *
         * If [force] is specified and the file doesn't exist, that error is ignored. (Other errors, eg. I/O errors or bad paths, are not ignored)
         *
         * This call cannot remove directories. Use [RmDirCommand] to remove an empty directory, or set [recursive] to remove directories recursively.
         */
        @GuestfishDsl fun rm(file: Path, force: Boolean = false, recursive: Boolean = false) =
            RmCommand(file, force, recursive).also { list.add(it) }

        /**
         * Remove the single [directory].
         */
        @GuestfishDsl fun rmDir(directory: Path) =
            RmDirCommand(directory).also { list.add(it) }

        /**
         * This unmounts all mounted filesystems.
         *
         * Some internal mounts are not unmounted by this call.
         */
        @GuestfishDsl fun umountAll() = UmountAllCommand().also { list.add(it) }

        /**
         * This exits guestfish.
         */
        @GuestfishDsl fun exit() = ExitCommand().also { list.add(it) }
    }
}

