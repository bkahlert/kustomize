package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.builder.OnOffBuilderInit
import com.bkahlert.koodies.builder.build
import com.bkahlert.koodies.builder.buildIfTo
import com.bkahlert.koodies.builder.buildList
import com.bkahlert.koodies.builder.buildListTo
import com.bkahlert.koodies.docker.DockerRunAdaptable
import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.libguestfs.Option
import com.imgcstmzr.libguestfs.SharedPath
import com.imgcstmzr.libguestfs.docker.GuestfishDockerAdaptable
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishCommandLineBuilder.Companion.build
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.libguestfs.resolveOnDocker
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
class GuestfishCommandLine(val env: Map<String, String>, val options: List<Option>, val guestfishCommands: List<GuestfishCommand>) :
    DockerRunAdaptable by GuestfishDockerAdaptable(env, options, guestfishCommands),
    LibguestfsCommandLine(
        env,
        options.filterIsInstance<GuestfishOption.DiskOption>().map { it.disk }.singleOrNull() ?: Paths.Temp,
        COMMAND,
        options.flatten() +
            guestfishCommands.toMutableList().let {
                if (UmountAllCommand() !in it) it += UmountAllCommand()
                if (ExitCommand() !in it) it += ExitCommand()
                it.flatten()
            }
    ) {

    fun RenderingLogger.executeLogging(): Int =
        executeLogging(caption = "Running ${guestfishCommands.size} guestfish operations... ${Kaomojis.fishing()}",
            ansiCode = ANSI.randomColor,
            nonBlockingReader = false,
            expectedExitValue = 0)

    companion object {
        const val COMMAND = "guestfish"

        @GuestfishDsl
        fun build(osImage: OperatingSystemImage, init: GuestfishCommandLineBuilder.() -> Unit) = init.build(osImage)

        @GuestfishDsl
        fun RenderingLogger.runGuestfishOn(osImage: OperatingSystemImage, init: GuestfishCommandsBuilder.() -> Unit): Int =
            build(osImage) {
                env {
                    trace { on }
                    debug { off }
                }

                options {
                    readWrite { on }
                    disk { it.file }
//                    inspector { on } // does not mount /boot
                    mount { Path.of("/dev/sda2") to Path.of("/") }
                    mount { Path.of("/dev/sda1") to Path.of("/boot") }
                }
                commands(init)
            }.run { executeLogging() }

        fun OperatingSystemImage.copyOut(path: String): Path {
            logging("copying out $path") {
                runGuestfishOn(this@copyOut) {
                    copyOut { it.resolveOnDisk(path) }
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
        private val env: MutableMap<String, String> = mutableMapOf(),
        private val options: MutableList<(OperatingSystemImage) -> GuestfishOption> = mutableListOf(),
        private val commands: MutableList<(OperatingSystemImage) -> GuestfishCommand> = mutableListOf(),
    ) {
        companion object {
            /**
             * Using `this` [GuestfishCommandLineBuilder] builds a list of [Option].
             */
            @GuestfishDsl fun (GuestfishCommandLineBuilder.() -> Unit).build(osImage: OperatingSystemImage): GuestfishCommandLine =
                GuestfishCommandLineBuilder().apply(this).let {
                    val options = it.options.map { it(osImage) }
                    val guestfishCommands = it.commands.map { it(osImage) }
                    GuestfishCommandLine(it.env, options, guestfishCommands)
                }
        }

        @GuestfishDsl
        fun env(init: GuestfishEnvironmentBuilder.() -> Unit) = init.buildList().forEach { env[it.first] = it.second }

        @GuestfishDsl
        fun options(init: GuestfishOptionsBuilder.() -> Unit) = init.buildListTo(options)

        @GuestfishDsl
        fun commands(init: GuestfishCommandsBuilder.() -> Unit) = init.buildListTo(commands)
    }

    @GuestfishDsl
    class GuestfishEnvironmentBuilder : ListBuilder<Pair<String, String>>() {

        /**
         * Enables command traces.
         */
        @GuestfishDsl
        fun trace(init: OnOffBuilderInit) =
            init.buildIfTo(list) { "LIBGUESTFS_TRACE" to "1" }

        /**
         * Enables verbose logging.
         */
        @GuestfishDsl
        fun debug(init: OnOffBuilderInit) =
            init.buildIfTo(list) { "LIBGUESTFS_DEBUG" to "1" }
    }

    @GuestfishDsl
    class GuestfishOptionsBuilder : ListBuilder<(OperatingSystemImage) -> GuestfishOption>() {

        /**
         * Use the this option to use guestfish safely if the disk image or virtual machine might be live.
         */
        @GuestfishDsl
        fun readOnly(init: OnOffBuilderInit) =
            init.buildIfTo(list) { { GuestfishOption.ReadOnlyOption() } }

        /**
         * Use the this option to explicitly allow write access. In future guestfish releases
         * [readOnly] might become the default setting.
         */
        @GuestfishDsl
        fun readWrite(init: OnOffBuilderInit) =
            init.buildIfTo(list) { { GuestfishOption.ReadWriteOption() } }

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        @GuestfishDsl
        fun disk(init: (OperatingSystemImage) -> Path) =
            list.add { init(it).run { GuestfishOption.DiskOption(this) } }

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        @GuestfishDsl
        fun inspector(init: OnOffBuilderInit) =
            init.buildIfTo(list) { { GuestfishOption.InspectorOption() } }

        /**
         * Mount the named partition or logical volume on the given mount point,
         * e.g. `Path.of("/dev/sda1") to Path.of("/")`
         */
        @GuestfishDsl
        fun mount(init: (OperatingSystemImage) -> Pair<Path, Path>) =
            list.add { init(it).run { GuestfishOption.MountOption(first, second) } }
    }

    @GuestfishDsl
    class GuestfishCommandsBuilder : ListBuilder<(OperatingSystemImage) -> GuestfishCommand>() {

        /**
         * All commands passed here will be treated as commands sent to the local shell
         * (`/bin/sh` or whatever [system(3)](http://man.he.net/man3/system) uses).
         */
        @GuestfishDsl fun runLocally(init: GuestfishCommandsBuilder.() -> Unit): List<(OperatingSystemImage) -> GuestfishCommand> =
            init.buildListTo(list) {
                { osImage: OperatingSystemImage ->
                    !this(osImage)
                }
            }

        /**
         * All command passed here will not cause guestfish to exit, even if any of these commands returns an error.
         */
        @GuestfishDsl fun ignoreErrors(init: GuestfishCommandsBuilder.() -> Unit): List<(OperatingSystemImage) -> GuestfishCommand> =
            init.buildListTo(list) {
                { osImage: OperatingSystemImage ->
                    -this(osImage)
                }
            }

        /**
         * This creates a raw command that calls [name] and passes [arguments].
         */
        @GuestfishDsl fun command(name: String, vararg arguments: String): GuestfishCommand =
            GuestfishCommand(name, *arguments).also { cmd -> list.add { cmd } }

        /**
         * This command copies local files or directories recursively into the disk image.
         */
        @GuestfishDsl fun copyIn(mkDir: Boolean = true, remoteDir: (OperatingSystemImage) -> Path) =
            list.add {
                remoteDir.build(it) {
                    CopyInCommand(
                        remoteDir = it.resolveOnDisk(this).parent,
                        mkDir = mkDir,
                        localFiles = arrayOf(it.resolveOnDocker(this)),
                    )
                }
            }

        /**
         * This command copies remote files or directories recursively out of the disk image.
         */
        @GuestfishDsl fun copyOut(mkDir: Boolean = true, remoteFile: (OperatingSystemImage) -> Path) =
            list.add {
                remoteFile.build(it) {
                    CopyOutCommand(
                        remoteFiles = arrayOf(it.resolveOnDisk(this)),
                        mkDir = mkDir,
                        directory = it.resolveOnDocker(this).parent,
                    )
                }
            }

        /**
         * This command uploads and unpacks the local [SharedPath.Host] directory to the [SharedPath.Disk] root directory.
         */
        @GuestfishDsl fun tarIn() =
            list.add {
                val rootDirectory = it.resolveOnHost("")
                val archiveOutsideRootDirectory = rootDirectory.tar(rootDirectory.parent.resolve("archive.tar"))
                archiveOutsideRootDirectory.moveTo(rootDirectory.resolve("archive.tar"))
                TarInCommand(
                    archive = it.resolveOnDocker("archive.tar"),
                    directory = it.resolveOnDisk(""),
                    deleteArchiveAfterwards = true,
                )
            }

        /**
         * * This command packs the root directory and downloads it to the [SharedPath.Host]'s shared directory.
         */
        @GuestfishDsl fun tarOut() =
            list.add {
                TarOutCommand(
                    directory = it.resolveOnDisk(""),
                    archive = it.resolveOnDocker("archive.tar"),
                )
            }


        /**
         * Touch acts like the [touch(1)](http://man.he.net/man1/touch) command.
         * It can be used to update the timestamps on a file, or, if the file does not exist, to create a new zero-length file.
         *
         * This command only works on regular files, and will fail on other file types such as directories, symbolic links, block special etc.
         */
        @GuestfishDsl fun touch(init: (OperatingSystemImage) -> Path) =
            list.add { init.build(it) { TouchCommand(this) } }

        /**
         * Remove the single [file].
         *
         * If [force] is specified and the file doesn't exist, that error is ignored. (Other errors, eg. I/O errors or bad paths, are not ignored)
         *
         * This call cannot remove directories. Use [RmDirCommand] to remove an empty directory, or set [recursive] to remove directories recursively.
         */
        @GuestfishDsl fun rm(force: Boolean = false, recursive: Boolean = false, file: (OperatingSystemImage) -> Path) =
            list.add { file.build(it) { RmCommand(this, force, recursive) } }

        /**
         * Remove the single [directory].
         */
        @GuestfishDsl fun rmDir(directory: (OperatingSystemImage) -> Path) =
            list.add { directory.build(it) { RmDirCommand(this) } }

        /**
         * This unmounts all mounted filesystems.
         *
         * Some internal mounts are not unmounted by this call.
         */
        @GuestfishDsl fun umountAll() = UmountAllCommand().also { cmd -> list.add { cmd } }

        /**
         * This exits guestfish.
         */
        @GuestfishDsl fun exit() = ExitCommand().also { cmd -> list.add { cmd } }
    }
}

