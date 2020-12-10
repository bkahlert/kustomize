package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.builder.ListBuilderInit
import com.bkahlert.koodies.builder.OnOffBuilderInit
import com.bkahlert.koodies.builder.buildListTo
import com.bkahlert.koodies.builder.buildTo
import com.bkahlert.koodies.docker.DockerRunAdaptable
import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.nio.file.serialized
import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.libguestfs.Option
import com.imgcstmzr.libguestfs.docker.GuestfishDockerAdaptable
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishCommandLineBuilder.Companion.build
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.log.RenderingLogger
import java.nio.file.Path

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

    protected val commandCount get() = lines.dropWhile { !it.startsWith("<<") }.size - 1
    override fun executionCaption() = "Running ${guestfishCommands.size} guestfish operations... ${Kaomojis.fishing()}"

    companion object {
        const val COMMAND = "guestfish"
        fun build(init: GuestfishCommandLineBuilder.() -> Unit) = init.build()

        fun RenderingLogger.fish(operatingSystemImage: OperatingSystemImage, init: GuestfishCommandsBuilder.() -> Unit): Int =
            build {
                options {
                    disks { +operatingSystemImage.file }
                    inspector { on }
                }
                commands(init)
            }.execute(this)
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
    class GuestfishCommandLineBuilder(private val options: MutableList<in GuestfishOption>, private val commands: MutableList<GuestfishCommand>) {
        companion object {
            /**
             * Using `this` [GuestfishCommandLineBuilder] builds a list of [Option].
             */
            fun (GuestfishCommandLineBuilder.() -> Unit).build(): GuestfishCommandLine =
                (mutableListOf<GuestfishOption>() to mutableListOf<GuestfishCommand>())
                    .also { (options, commands) -> GuestfishCommandLineBuilder(options, commands).apply(this) }
                    .let { (options, commands) -> GuestfishCommandLine(options, commands) }
        }

        fun options(init: GuestfishOptionsBuilder.() -> Unit) = init.buildListTo(options)
        fun commands(init: GuestfishCommandsBuilder.() -> Unit) = init.buildListTo(commands)
    }

    @GuestfishDsl
    class GuestfishOptionsBuilder : ListBuilder<GuestfishOption>() {

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        fun disks(init: ListBuilderInit<Path>) =
            init.buildListTo(list) { GuestfishOption.DiskOption(this) }

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        fun inspector(init: OnOffBuilderInit) =
            init.buildTo(list) { GuestfishOption.InspectorOption() }
    }

    @GuestfishDsl
    class GuestfishCommandsBuilder : ListBuilder<GuestfishCommand>() {

        /**
         * All commands passed here will be treated as commands sent to the local shell
         * (`/bin/sh` or whatever [system(3)](http://man.he.net/man3/system) uses).
         */
        fun runLocally(init: GuestfishCommandsBuilder.() -> Unit) {
            init.buildListTo(list) { !this }
        }

        /**
         * All command passed here will not cause guestfish to exit, even if any of these commands returns an error.
         */
        fun ignoreErrors(init: GuestfishCommandsBuilder.() -> Unit) {
            init.buildListTo(list) { -this }
        }

        /**
         * This creates a raw command that calls [name] and passes [arguments].
         */
        fun command(name: String, vararg arguments: String) =
            list.add(GuestfishCommand(name, *arguments))

        /**
         * This command copies local files or directories recursively into the disk image.
         */
        fun copyIn(mkdir: Boolean = true, init: () -> CopyInCommand): CopyInCommand {
            return init.buildTo(list) {
                if (mkdir) list.add(GuestfishCommand("-mkdir-p", remoteDir.serialized))
                this
            }
        }

        /**
         * This command copies remote files or directories recursively out of the disk image.
         */
        fun copyOut(mkdir: Boolean = true, init: () -> CopyOutCommand): CopyOutCommand {
            return init.buildTo(list) {
                if (mkdir) list.add(GuestfishCommand("!mkdir", "-p", directory.serialized))
                this
            }
        }

        /**
         * This command uploads and unpacks the a local tar file into a directory.
         */
        fun tarIn(init: () -> TarInCommand) =
            init.buildTo(list)

        /**
         * This command packs the contents of a directory and downloads it to a local tar file.
         */
        fun tarOut(init: () -> TarOutCommand) =
            init.buildTo(list)

        /**
         * This unmounts all mounted filesystems.
         *
         * Some internal mounts are not unmounted by this call.
         */
        fun umountAll(init: () -> UmountAllCommand) =
            init.buildTo(list)

        /**
         * This exits guestfish.
         */
        fun exit(init: () -> ExitCommand) =
            init.buildTo(list)
    }
}

