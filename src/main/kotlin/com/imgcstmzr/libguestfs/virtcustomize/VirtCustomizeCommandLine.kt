package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeCommandLineBuilder.Companion.build
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.AppendLineOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.ChmodOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CommandsFromFileOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyInOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.DeleteOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.EditOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootCommandOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootInstallOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.HostnameOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.MkdirOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.MoveOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.PasswordOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.RootPasswordOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.SshInjectOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.TimeZoneOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.TouchOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.WriteOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.ColorsOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.DiskOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.QuietOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.TraceOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.VerboseOption
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.builder.ListBuilder
import koodies.builder.OnOffBuilderInit
import koodies.builder.build
import koodies.builder.buildIfTo
import koodies.builder.buildList
import koodies.builder.buildListTo
import koodies.callableOnce
import koodies.io.noSuchFile
import koodies.io.path.withDirectoriesCreated
import koodies.io.requireClassPath
import koodies.logging.RenderingLogger
import koodies.shell.ShellScript
import koodies.terminal.ANSI
import koodies.text.withRandomSuffix
import java.nio.file.Path
import java.util.TimeZone
import kotlin.io.path.isReadable
import kotlin.io.path.readText
import kotlin.io.path.writeLines

@DslMarker
annotation class VirtCustomizeDsl

/**
 * A virt-customize command can customize a virtual machine respectively its disk image by installing packages,
 * editing configuration files, and so on.
 *
 * The [FirstBootOption] and [FirstBootCommandOption] allow you to execute commands at the first boot of the guest.
 * To do so, an init script for the guest init system is installed, which takes care of running all the added scripts and commands.
 *
 * Supported operating systems are:
 *
 * * Linux
 *   Init systems supported are: systemd, System-V init (known also as sysvinit), and Upstart (using the System-V scripts).
 *
 *   Note that usually init scripts run as root, but with a more limited environment than what could be available from a normal shell:
 *      for example, $HOME may be unset or empty.
 *
 *   The output of the first boot scripts is available in the guest as ~root/virt-sysprep-firstBoot.log.
 *
 * * Windows
 *   rhsrvany.exe, available from sources at https://github.com/rwmjones/rhsrvany, or pvvxsvc.exe, available with SUSE VMDP
 *   is installed to run the first boot scripts. It is required, and the setup of first boot scripts will fail if it is not present.
 *
 *   rhsrvany.exe or pvvxsvc.exe is copied from the location pointed to by the VIRT_TOOLS_DATA_DIR environment variable;
 *   if not set, a compiled-in default will be used (something like /usr/share/virt-tools).
 *
 *   The output of the first boot scripts is available in the guest as C:\Program Files\Guestfs\FirstBoot\log.txt.
 *
 * @see <a href="https://libguestfs.org/virt-builder.1.html#first-boot-scripts">First Boot Scripts</a>
 * @see <a href="https://libguestfs.org/virt-customize.1.html">virt-customize - Customize a virtual machine</a>
 */
class VirtCustomizeCommandLine(
    val options: List<VirtCustomizeOption>,
    val customizationOptions: List<VirtCustomizeCustomizationOption>,
) : LibguestfsCommandLine(
    emptyMap(),
    options.filterIsInstance<DiskOption>().map { it.disk }.single(),
    COMMAND,
    options.flatten() + customizationOptions.flatten()) {

    override val disk = options.filterIsInstance<DiskOption>().map { it.disk }
        .also { disks -> check(disks.size == 1) { "The $command command must add exactly one disk. ${disks.size} found: ${disks.joinToString(", ")}." } }
        .single().also { check(it.isReadable()) { it.noSuchFile() } }

    fun RenderingLogger.executeLogging(): Int =
        executeLogging(caption = "Running $summary...", ansiCode = ANSI.termColors.brightBlue, nonBlockingReader = false, expectedExitValue = 0)

    companion object {
        const val COMMAND = "virt-customize"

        @VirtCustomizeDsl
        fun build(osImage: OperatingSystemImage, init: VirtCustomizeCommandLineBuilder.() -> Unit) = init.build(osImage)

        @VirtCustomizeDsl
        fun RenderingLogger.virtCustomize(
            osImage: OperatingSystemImage,
            trace: Boolean = false,
            init: VirtCustomizeCustomizationOptionsBuilder.() -> Unit,
        ): Int = build(osImage) {
            options {
                colors { on }
                disk { it.file }
                if (trace) trace { on }
            }
            customizationOptions(init)
        }.run { executeLogging() }
    }

    @VirtCustomizeDsl
    class VirtCustomizeCommandLineBuilder(
        private val options: MutableList<(OperatingSystemImage) -> VirtCustomizeOption> = mutableListOf(),
        private val customizationOptions: MutableList<(OperatingSystemImage) -> VirtCustomizeCustomizationOption> = mutableListOf(),
    ) {
        companion object {
            @VirtCustomizeDsl
            fun (VirtCustomizeCommandLineBuilder.() -> Unit).build(osImage: OperatingSystemImage): VirtCustomizeCommandLine =
                VirtCustomizeCommandLineBuilder().apply(this).let { builder ->
                    val options = builder.options.map { it(osImage) }
                    val customizationOptions = builder.customizationOptions.map { it(osImage) }
                    VirtCustomizeCommandLine(options, customizationOptions)
                }
        }

        fun options(init: VirtCustomizeOptionsBuilder.() -> Unit) {
            init.buildListTo(options)
        }

        fun customizationOptions(init: VirtCustomizeCustomizationOptionsBuilder.() -> Unit) {
            init.buildListTo(customizationOptions)
        }
    }

    @VirtCustomizeDsl
    class VirtCustomizeOptionsBuilder : ListBuilder<(OperatingSystemImage) -> VirtCustomizeOption>() {

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        fun disk(init: (OperatingSystemImage) -> Path) =
            list.add { init.build(it) { DiskOption(this) } }

        /**
         * Use ANSI colour sequences to colourize messages.
         * This is the default when the output is a tty.
         *
         * If the output of the program is redirected to a file, ANSI colour sequences are disabled unless you use this option.
         */
        fun colors(init: OnOffBuilderInit) =
            init.buildIfTo(list) { { ColorsOption() } }

        /**
         * Don’t print log messages.
         *
         * To enable detailed logging of individual file operations, use -x.
         */
        fun quiet(init: OnOffBuilderInit) =
            init.buildIfTo(list) { { QuietOption() } }

        /**
         * Display version number and exit.
         */
        fun verbose(init: OnOffBuilderInit) =
            init.buildIfTo(list) { { VerboseOption() } }

        /**
         * Enable tracing of libguestfs API calls.
         */
        fun trace(init: OnOffBuilderInit) =
            init.buildIfTo(list) { { TraceOption() } }
    }

    @VirtCustomizeDsl
    class VirtCustomizeCustomizationOptionsBuilder :
        ListBuilder<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>() {

        private val fixFirstBootOrder by callableOnce {
            fileWithText(FIRSTBOOT_FIX, requireClassPath(FIRSTBOOT_FIX.path) { readText() })
            chmods { "0755" to FIRSTBOOT_FIX }
        }

        /**
         * Append a single line of text to the specified [DiskPath].
         *
         * If the file does not already end with a newline, then one is added before the appended line.
         * Also a newline is added to the end of the specified line automatically.
         */
        fun appendLine(init: (OperatingSystemImage) -> Pair<String, DiskPath>) =
            list.add { init(it).run { AppendLineOption(second, first) } }

        /**
         * Change the permissions of FILE to PERMISSIONS.
         *
         * *Note:* PERMISSIONS by default would be decimal, unless you prefix it with 0 to get octal, ie. use 0700 not 700.
         */
        fun chmods(init: (OperatingSystemImage) -> Pair<String, DiskPath>) =
            list.add { init(it).run { ChmodOption(first, second) } }

        /**
         * Read the customize commands from a file, one (and its arguments) each line.
         *
         * Each line contains a single customization command and its arguments
         */
        fun commandsFromFiles(init: (OperatingSystemImage) -> HostPath) =
            list.add { init.build(it) { CommandsFromFileOption(this) } }

        /**
         * Copy files or directories recursively inside the guest.
         *
         * Wildcards cannot be used.
         */
        fun copy(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) =
            list.add { init.build(it) { CopyOption(first, second) } }

        /**
         * Copy local files or directories recursively into the disk image, placing them in the directory REMOTEDIR
         * (which must exist).
         *
         * Wildcards cannot be used.
         */
        fun copyIn(init: (OperatingSystemImage) -> Pair<HostPath, DiskPath>): Boolean {
            list.add { init.build(it) { MkdirOption(second) } }
            return list.add { init.build(it) { CopyInOption(first, second) } }
        }

        /**
         * Copies the disk absolute [diskPath] (e.g. `/etc/hostname`) located in the
         * shared directory into the disk image.
         *
         * If [diskPath] points to a directory, the directory is copied recursively.
         *
         * Before the [diskPath] is copied, it will be processed by the optional [init],
         * which can be used to created appropriate resources. [init] has the [Path] as
         * its receiver object which will be copied.
         */
        fun copyIn(diskPath: DiskPath, init: HostPath.() -> Unit = {}) =
            copyIn { osImage ->
                require(diskPath.isAbsolute) { "$diskPath must be absolute but is not." }
                val hostPath = osImage.hostPath(diskPath)
                init(hostPath)
                hostPath to diskPath.parent
            }

        /**
         * Convenience function to [copyIn] a file with [text] as its content
         * under the specified [diskPath].
         */
        fun fileWithText(diskPath: DiskPath, text: String) =
            copyIn(diskPath) { withDirectoriesCreated().writeLines(text.lines()) }

        /**
         * Delete a file from the guest. Or delete a directory (and all its contents, recursively).
         */
        fun delete(init: (OperatingSystemImage) -> DiskPath) =
            list.add { init.build(it) { DeleteOption(this) } }

        /**
         * Edit FILE using the Perl expression EXPR.
         */
        fun edit(init: (OperatingSystemImage) -> Pair<DiskPath, String>) =
            list.add { init.build(it) { EditOption(first, second) } }


        /**
         * Set the hostname of the guest to HOSTNAME. You can use a dotted hostname.domainname (FQDN) if you want.
         */
        fun hostname(init: (OperatingSystemImage) -> String) =
            list.add { init(it).run { HostnameOption(this) } }

        /**
         * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
         */
        fun firstBoot(shellScript: ShellScript): Boolean {
            fixFirstBootOrder()
            return list.add { osImage -> firstBoot(osImage, shellScript) }
        }

        /**
         * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
         */
        fun firstBoot(name: String? = null, init: ShellScript.(OperatingSystemImage) -> Unit): Boolean {
            fixFirstBootOrder()
            return list.add { osImage -> firstBoot(osImage, ShellScript(name) { init(osImage) }) }
        }

        private fun firstBoot(osImage: OperatingSystemImage, shellScript: ShellScript): FirstBootOption =
            DiskPath("/script".withRandomSuffix() + ".sh")
                .let { diskPath -> osImage.hostPath(diskPath) }
                .let { hostPath -> shellScript.buildTo(hostPath) }
                .let { scriptFile -> FirstBootOption(scriptFile) }

        /**
         * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
         */
        fun firstBootCommand(init: (OperatingSystemImage) -> String): Boolean {
            fixFirstBootOrder()
            return list.add { init(it).run { FirstBootCommandOption(this) } }
        }

        /**
         * Install the named packages (a comma-separated list).
         * These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.)
         * and the guest’s network connection.
         */
        fun firstBootInstall(init: ListBuilder<String>.(OperatingSystemImage) -> Unit): Boolean {
            fixFirstBootOrder()
            return list.add { init.buildList(it).run { FirstBootInstallOption(this) } }
        }

        /**
         * Create a directory in the guest.
         *
         * This uses mkdir -p so any intermediate directories are created, and it also works if the directory already exists.
         */
        fun mkdir(init: (OperatingSystemImage) -> DiskPath) =
            list.add { init(it).run { MkdirOption(this) } }

        /**
         * Move files or directories inside the guest.
         *
         * Wildcards cannot be used.
         */
        fun move(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) =
            list.add { init(it).run { MoveOption(first, second) } }

        /**
         * Set the password for USER. (Note this option does not create the user account).
         */
        fun password(init: () -> PasswordOption) =
            list.add { init() }

        /**
         * Set the root password.
         */
        fun rootPassword(init: () -> RootPasswordOption) =
            list.add { init() }

        /**
         * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
         *
         * The USER must exist already in the guest.
         */
        fun sshInjectFile(init: (OperatingSystemImage) -> Pair<String, HostPath>) =
            list.add { init(it).run { SshInjectOption(first, second) } }

        /**
         * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
         *
         * The USER must exist already in the guest.
         */
        fun sshInject(username: String, password: String) =
            list.add { SshInjectOption(username, password) }

        /**
         * Sets the time zone.
         *
         * Example: `Europe/Berlin`
         */
        fun timeZone(timezone: TimeZone) =
            list.add { TimeZoneOption(timezone) }

        /**
         * Sets the time zone.
         *
         * Example: `Europe/Berlin`
         */
        fun timeZoneId(timezoneId: String) =
            list.add { TimeZoneOption(timezoneId) }

        /**
         * This command performs a touch(1)-like operation on FILE.
         */
        fun touch(init: (OperatingSystemImage) -> DiskPath) =
            list.add { init(it).run { TouchOption(this) } }

        /**
         * Write CONTENT to FILE.
         */
        fun write(init: (OperatingSystemImage) -> Pair<DiskPath, String>) =
            list.add { init(it).run { WriteOption(first, second) } }

        companion object {
            val VIRT_SYSPREP = DiskPath("/usr/lib/virt-sysprep")
            val FIRSTBOOT_SCRIPTS = VIRT_SYSPREP.resolve("scripts")
            val FIRSTBOOT_FIX = FIRSTBOOT_SCRIPTS.resolve("0000---fix-order---")
        }
    }
}
