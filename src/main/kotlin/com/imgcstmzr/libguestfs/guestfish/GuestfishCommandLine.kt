package com.imgcstmzr.libguestfs.guestfish

import com.imgcstmzr.libguestfs.*
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.GuestfishCommandLineContext
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishCommandsBuilder.GuestfishCommandsContext
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishEnvironmentBuilder.GuestfishEnvironmentContext
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishOptionsBuilder.GuestfishOptionsContext
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption.DiskOption
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption.InspectorOption
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption.MountOption
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption.ReadOnlyOption
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption.ReadWriteOption
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.builder.*
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.collections.requireContainsSingleOfType
import koodies.concurrent.execute
import koodies.io.compress.TarArchiver.tar
import koodies.io.noSuchFile
import koodies.kaomoji.Kaomojis
import koodies.logging.LoggingOptions
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.shell.HereDocBuilder
import koodies.terminal.ANSI
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.moveTo

@DslMarker
annotation class GuestfishDsl

/**
 * Guestfish is a shell and command-line tool for examining and modifying virtual machine filesystems.
 *
 * It uses [Libguestfs] and exposes all of the functionality of the [guestfs API](https://libguestfs.org/guestfs.3.html).
 */
class GuestfishCommandLine(
    val env: Map<String, String>,
    val options: List<Option>,
    val guestfishCommands: List<GuestfishCommand>,
) : LibguestfsCommandLine(
    environment = env,
    disk = options.requireContainsSingleOfType<DiskOption>().disk,
    command = COMMAND,
    arguments = options.filterIsInstance<DiskOption>().flatten() +
        options.filter { it !is DiskOption }.flatten() +
        "--" + HereDocBuilder.hereDoc {
        guestfishCommands.toMutableList().also {
            if (options.filterIsInstance<MountOption>().isNotEmpty() && UmountAllCommand() !in it) it.add(UmountAllCommand())
            if (ExitCommand() !in it) it.add(ExitCommand())
        }.forEach {
            val disk = options.requireContainsSingleOfType<DiskOption>().disk
            +it.joinToString(" ") { arg -> relativize(disk, arg) }
        }
    }
) {

    override val disk = options.filterIsInstance<DiskOption>().map { it.disk }
        .also { disks -> check(disks.size == 1) { "The $command command must add exactly one disk. ${disks.size} found: ${disks.joinToString(", ")}." } }
        .single().also { check(it.isReadable()) { it.noSuchFile() } }

    companion object : BuilderTemplate<GuestfishCommandLineContext, (OperatingSystemImage) -> GuestfishCommandLine>() {
        @GuestfishDsl
        class GuestfishCommandLineContext(override val captures: CapturesMap) : CapturingContext() {

            val env by GuestfishEnvironmentBuilder default { emptyList<(OperatingSystemImage) -> Pair<String, String>>() }
            val options by GuestfishOptionsBuilder default { emptyList<(OperatingSystemImage) -> GuestfishOption>() }
            val commands by GuestfishCommandsBuilder default { emptyList<(OperatingSystemImage) -> GuestfishCommand>() }
        }

        override fun BuildContext.build(): (OperatingSystemImage) -> GuestfishCommandLine = ::GuestfishCommandLineContext {
            { osImage: OperatingSystemImage ->
                val env: List<(OperatingSystemImage) -> Pair<String, String>> = ::env.eval()
                val options: List<(OperatingSystemImage) -> GuestfishOption> = ::options.eval()
                val commands: List<(OperatingSystemImage) -> GuestfishCommand> = ::commands.eval()
                GuestfishCommandLine(env.map { it(osImage) }.toMap(), options.map { it(osImage) }, commands.map { it(osImage) })
            }
        }

        const val COMMAND = "guestfish"
        val DEFAULT_CAPTION: List<GuestfishCommand>.() -> String = { "Running $size guestfish operations … ${Kaomojis.fishing()}" }

        @GuestfishDsl
        fun build(osImage: OperatingSystemImage, init: Init<GuestfishCommandLineContext>) = build(init)(osImage)

        @GuestfishDsl
        fun RenderingLogger.guestfish(
            osImage: OperatingSystemImage,
            trace: Boolean = false,
            caption: List<GuestfishCommand>.() -> String = DEFAULT_CAPTION,
            bordered: Boolean = false,
            init: Init<GuestfishCommandsContext>,
        ): Int = build(osImage) {
            env {
                this.trace by trace
                debug { off }
            }

            options {
                readWrite { on }
                disk { it.file }
                //                    inspector { on } // does not mount /boot
                mount { Path.of("/dev/sda2") to DiskPath("/") }
                mount { Path.of("/dev/sda1") to DiskPath("/boot") }
            }
            commands(init)
        }.run { dockerCommandLine().execute(0, null, LoggingOptions(caption(guestfishCommands), ANSI.termColors.blue, bordered)).waitForTermination() }

        fun OperatingSystemImage.copyOut(path: String, trace: Boolean = false): Path {
            return logging("copying out $path") {
                guestfish(this@copyOut, trace = trace) {
                    copyOut { DiskPath(path) }
                }
                hostPath(DiskPath(path))
            }
        }
    }


    object GuestfishEnvironmentBuilder : BuilderTemplate<GuestfishEnvironmentContext, List<(OperatingSystemImage) -> Pair<String, String>>>() {

        @GuestfishDsl
        class GuestfishEnvironmentContext(override val captures: CapturesMap) : CapturingContext() {

            val env by function<(OperatingSystemImage) -> Pair<String, String>>()

            /**
             * Enables command traces.
             */
            val trace by BooleanBuilder.OnOff delegate { if (it) env { "LIBGUESTFS_TRACE" to "1" } }

            /**
             * Enables verbose logging.
             */
            val debug by BooleanBuilder.OnOff delegate { if (it) env { "LIBGUESTFS_DEBUG" to "1" } }
        }

        override fun BuildContext.build() = ::GuestfishEnvironmentContext{ ::env.evalAll() }
    }

    object GuestfishOptionsBuilder : BuilderTemplate<GuestfishOptionsContext, List<(OperatingSystemImage) -> GuestfishOption>>() {

        @GuestfishDsl
        class GuestfishOptionsContext(override val captures: CapturesMap) : CapturingContext() {

            val option by function<(OperatingSystemImage) -> GuestfishOption>()

            /**
             * Use the this option to use guestfish safely if the disk image or virtual machine might be live.
             */
            val readOnly by BooleanBuilder.OnOff delegate {
                if (it) option { ReadOnlyOption() }
            }

            /**
             * Use the this option to explicitly allow write access. In future guestfish releases
             * [readOnly] might become the default setting.
             */
            val readWrite by BooleanBuilder.OnOff delegate {
                if (it) option { ReadWriteOption() }
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
            val inspector by BooleanBuilder.OnOff delegate {
                if (it) option { InspectorOption() }
            }

            /**
             * Mount the named partition or logical volume on the given mount point,
             * e.g. `Path.of("/dev/sda1") to Path.of("/")`
             */
            fun mount(init: (OperatingSystemImage) -> Pair<HostPath, DiskPath>) {
                option { init(it).run { MountOption(first, second) } }
            }
        }

        override fun BuildContext.build() = ::GuestfishOptionsContext { ::option.evalAll() }
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
                command { remoteDir(it).run { CopyInCommand(mkDir, parent ?: this, it.hostPath(this)) } }
            }

            /**
             * This command copies remote files or directories recursively out of the disk image.
             */
            fun copyOut(mkDir: Boolean = true, remoteFile: (OperatingSystemImage) -> DiskPath) {
                command { remoteFile(it).run { CopyOutCommand(mkDir, it.hostPath(this).parent ?: it.hostPath(this), this) } }
            }

            /**
             * This command uploads and unpacks the local host share to the guest VMs root `/`.
             */
            fun tarIn(diskPath: (OperatingSystemImage) -> DiskPath = { DiskPath("/") }) {
                command { diskPath(it).run { TarInCommand(true, tar(it), this) } }
            }

            /**
             * Creates a tar archive of `this` [DiskPath] and places the archive inside of this path.
             */
            private fun DiskPath.tar(osImage: OperatingSystemImage, archiveName: String = "archive.tar"): Path =
                osImage.hostPath(this).run { resolve(archiveName).also { target -> tar(parent.resolve(archiveName)).moveTo(target) } }

            /**
             * * This command packs the guest VMs root `/` and downloads it to the local host share.
             */
            fun tarOut() {
                command { TarOutCommand(DiskPath("/"), it.hostPath(DiskPath("/archive.tar"))) }
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
                command { RmCommand(file().toString(), force, recursive) }
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
                command { UmountAllCommand() }
            }

            /**
             * This exits guestfish.
             */
            fun exit() {
                command { ExitCommand() }
            }
        }

        override fun BuildContext.build() = ::GuestfishCommandsContext{ ::command.evalAll() }
    }
}

