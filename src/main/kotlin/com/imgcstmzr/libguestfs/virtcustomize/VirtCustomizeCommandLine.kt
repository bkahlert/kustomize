package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.VirtCustomizeCommandLineContext
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeCustomizationOptionsBuilder.VirtCustomizeCustomizationOptionsContext
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeOptionsBuilder.VirtCustomizeOptionsContext
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
import koodies.builder.BooleanBuilder
import koodies.builder.BuilderTemplate
import koodies.builder.build
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.callableOnce
import koodies.collections.head
import koodies.collections.requireContainsSingleOfType
import koodies.collections.tail
import koodies.concurrent.execute
import koodies.io.noSuchFile
import koodies.io.path.withDirectoriesCreated
import koodies.logging.LoggingOptions
import koodies.logging.RenderingLogger
import koodies.logging.asStatus
import koodies.shell.ShellScript
import koodies.terminal.ANSI
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import koodies.text.truncateTo
import koodies.text.withRandomSuffix
import java.nio.file.Path
import java.util.TimeZone
import kotlin.io.path.isReadable
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
    environment = emptyMap(),
    disk = options.requireContainsSingleOfType<DiskOption>().disk,
    command = COMMAND,
    arguments = options.requireContainsSingleOfType<DiskOption>().disk.let { disk ->
        options.flatten() + customizationOptions.flatMap { it.map { arg -> relativize(disk, arg) } }
    }) {

    override val disk = options.requireContainsSingleOfType<DiskOption>().disk
        .also { check(it.isReadable()) { it.noSuchFile() } }

    override val summary: String
        get() = customizationOptions
            .map { option -> "${option.arguments.head}(${option.arguments.tail.joinToString(", ")})" }
            .run { map { it.truncateTo(25).truncate(25, MIDDLE).toString() } }
            .asStatus()

    companion object : BuilderTemplate<VirtCustomizeCommandLineContext, (OperatingSystemImage) -> VirtCustomizeCommandLine>() {
        @VirtCustomizeDsl
        class VirtCustomizeCommandLineContext(override val captures: CapturesMap) : CapturingContext() {
            val options by VirtCustomizeOptionsBuilder
            val customizationOptions by VirtCustomizeCustomizationOptionsBuilder
        }

        override fun BuildContext.build() = ::VirtCustomizeCommandLineContext {
            { osImage: OperatingSystemImage ->
                val options: List<(OperatingSystemImage) -> VirtCustomizeOption> = ::options.eval()
                val customizationOptions: List<(OperatingSystemImage) -> VirtCustomizeCustomizationOption> = ::customizationOptions.eval()
                VirtCustomizeCommandLine(options.map { it(osImage) }, customizationOptions.map { it(osImage) })
            }
        }

        const val COMMAND = "virt-customize"

        @VirtCustomizeDsl
        fun build(osImage: OperatingSystemImage, init: VirtCustomizeCommandLineContext.() -> Unit) = build(init)(osImage)

        @VirtCustomizeDsl
        fun RenderingLogger.virtCustomize(
            osImage: OperatingSystemImage,
            trace: Boolean = false,
            init: VirtCustomizeCustomizationOptionsContext.() -> Unit,
        ): Int = build(osImage) {
            options {
                colors { on }
                disk { it.file }
                if (trace) trace { on }
            }
            customizationOptions(init)
        }.run { dockerCommandLine().execute(0, null, LoggingOptions("Running $summary…", ANSI.termColors.brightBlue)).waitForTermination() }
    }

    object VirtCustomizeOptionsBuilder : BuilderTemplate<VirtCustomizeOptionsContext, List<(OperatingSystemImage) -> VirtCustomizeOption>>() {
        @VirtCustomizeDsl
        class VirtCustomizeOptionsContext(override val captures: CapturesMap) : CapturingContext() {
            val option by function<(OperatingSystemImage) -> VirtCustomizeOption>()

            /**
             * Add file which should be a disk image from a virtual machine.
             *
             * The format of the disk image is auto-detected.
             */
            fun disk(init: (OperatingSystemImage) -> Path) {
                option { init(it).run { DiskOption(this) } }
            }

            /**
             * Use ANSI colour sequences to colourize messages.
             * This is the default when the output is a tty.
             *
             * If the output of the program is redirected to a file, ANSI colour sequences are disabled unless you use this option.
             */
            val colors by BooleanBuilder.OnOff delegate {
                if (it) option { ColorsOption() }
            }

            /**
             * Don’t print log messages.
             *
             * To enable detailed logging of individual file operations, use -x.
             */
            val quiet by BooleanBuilder.OnOff delegate {
                if (it) option { QuietOption() }
            }

            /**
             * Display version number and exit.
             */
            val verbose by BooleanBuilder.OnOff delegate {
                if (it) option { VerboseOption() }
            }

            /**
             * Enable tracing of libguestfs API calls.
             */
            val trace by BooleanBuilder.OnOff delegate {
                if (it) option { TraceOption() }
            }
        }

        override fun BuildContext.build() = ::VirtCustomizeOptionsContext { evalAll() }
    }

    object VirtCustomizeCustomizationOptionsBuilder :
        BuilderTemplate<VirtCustomizeCustomizationOptionsContext, List<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>>() {

        @VirtCustomizeDsl
        class VirtCustomizeCustomizationOptionsContext(override val captures: CapturesMap) : CapturingContext() {

            private val fixFirstBootOrder by callableOnce {
                copyIn(FirstBootFix.FIRSTBOOT_FIX, FirstBootFix.text)
                chmods { "0755" to FirstBootFix.FIRSTBOOT_FIX }
            }

            val customizationOption by function<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>()

            /**
             * Append a single line of text to the specified [DiskPath].
             *
             * If the file does not already end with a newline, then one is added before the appended line.
             * Also a newline is added to the end of the specified line automatically.
             */
            fun appendLine(init: (OperatingSystemImage) -> Pair<String, DiskPath>) {
                customizationOption { init(it).run { AppendLineOption(second, first) } }
            }

            /**
             * Change the permissions of FILE to PERMISSIONS.
             *
             * *Note:* PERMISSIONS by default would be decimal, unless you prefix it with 0 to get octal, ie. use 0700 not 700.
             */
            fun chmods(init: (OperatingSystemImage) -> Pair<String, DiskPath>) {
                customizationOption { init(it).run { ChmodOption(first, second) } }
            }

            /**
             * Read the customize commands from a file, one (and its arguments) each line.
             *
             * Each line contains a single customization command and its arguments
             */
            fun commandsFromFiles(init: (OperatingSystemImage) -> HostPath) {
                customizationOption { init(it).run { CommandsFromFileOption(this) } }
            }

            /**
             * Copy files or directories recursively inside the guest.
             *
             * Wildcards cannot be used.
             */
            fun copy(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) {
                customizationOption { init(it).run { CopyOption(first, second) } }
            }

            /**
             * Copy local files or directories recursively into the disk image, placing them in the directory REMOTEDIR
             * (which must exist).
             *
             * Wildcards cannot be used.
             */
            fun copyIn(init: (OperatingSystemImage) -> Pair<HostPath, DiskPath>) {
                customizationOption { init(it).run { MkdirOption(second) } }
                customizationOption { init(it).run { CopyInOption(first, second) } }
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
            fun copyIn(diskPath: DiskPath, init: HostPath.() -> Unit = {}) {
                copyIn { osImage ->
                    require(diskPath.isAbsolute) { "$diskPath must be absolute but is not." }
                    val hostPath = osImage.hostPath(diskPath)
                    init(hostPath)
                    hostPath to (diskPath.parent ?: diskPath)
                }
            }

            /**
             * Convenience function to [copyIn] a file with [text] as its content
             * under the specified [diskPath].
             */
            fun copyIn(diskPath: DiskPath, text: String) {
                copyIn(diskPath) { withDirectoriesCreated().writeLines(text.lines()) }
            }

            /**
             * Delete a file from the guest. Or delete a directory (and all its contents, recursively).
             */
            fun delete(init: (OperatingSystemImage) -> DiskPath) {
                customizationOption { init(it).run { DeleteOption(this) } }
            }

            /**
             * Edit FILE using the Perl expression EXPR.
             */
            fun edit(init: (OperatingSystemImage) -> Pair<DiskPath, String>) {
                customizationOption { init(it).run { EditOption(first, second) } }
            }

            /**
             * Set the hostname of the guest to HOSTNAME. You can use a dotted hostname.domainname (FQDN) if you want.
             */
            fun hostname(init: (OperatingSystemImage) -> String) {
                customizationOption { init(it).run { HostnameOption(this) } }
            }

            /**
             * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
             */
            fun firstBoot(shellScript: ShellScript) {
                fixFirstBootOrder()
                customizationOption { osImage: OperatingSystemImage ->
                    val diskPath = DiskPath("/script".withRandomSuffix() + ".sh")
                    val hostPath = osImage.hostPath(diskPath)
                    val scriptFile = shellScript.buildTo(hostPath)
                    FirstBootOption(scriptFile)
                }
            }

            /**
             * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
             */
            fun firstBoot(name: String? = null, init: ShellScript.(OperatingSystemImage) -> Unit) {
                fixFirstBootOrder()
                customizationOption { osImage: OperatingSystemImage ->
                    val diskPath = DiskPath("/script".withRandomSuffix() + ".sh")
                    val hostPath = osImage.hostPath(diskPath)
                    val scriptFile = ShellScript(name) { init(osImage) }.buildTo(hostPath)
                    FirstBootOption(scriptFile)
                }
            }

            /**
             * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
             */
            fun firstBootCommand(init: (OperatingSystemImage) -> String) {
                fixFirstBootOrder()
                customizationOption { init(it).run { FirstBootCommandOption(this) } }
            }

            /**
             * Install the named packages (a comma-separated list).
             * These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.)
             * and the guest’s network connection.
             */

            fun firstBootInstall(init: (OperatingSystemImage) -> List<String>) {
                fixFirstBootOrder
                customizationOption { init(it).run { FirstBootInstallOption(this) } }
            }

            /**
             * Create a directory in the guest.
             *
             * This uses mkdir -p so any intermediate directories are created, and it also works if the directory already exists.
             */
            fun mkdir(init: (OperatingSystemImage) -> DiskPath) {
                customizationOption { init(it).run { MkdirOption(this) } }
            }

            /**
             * Move files or directories inside the guest.
             *
             * Wildcards cannot be used.
             */
            fun move(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) {
                customizationOption { init(it).run { MoveOption(first, second) } }
            }

            /**
             * Set the password for USER. (Note this option does not create the user account).
             */

            fun password(passwordOption: PasswordOption) {
                customizationOption { passwordOption }
            }

            /**
             * Set the root password.
             */
            fun rootPassword(rootPasswordOption: RootPasswordOption) {
                customizationOption { rootPasswordOption }
            }

            /**
             * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
             *
             * The USER must exist already in the guest.
             */
            fun sshInjectFile(init: (OperatingSystemImage) -> Pair<String, HostPath>) {
                customizationOption { init(it).run { SshInjectOption(first, second) } }
            }

            /**
             * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
             *
             * The USER must exist already in the guest.
             */

            fun sshInject(init: (OperatingSystemImage) -> Pair<String, String>) {
                customizationOption { init(it).run { SshInjectOption(first, second) } }
            }

            /**
             * Sets the time zone.
             *
             * Example: `Europe/Berlin`
             */
            fun timeZone(init: (OperatingSystemImage) -> TimeZone) {
                customizationOption { init(it).run { TimeZoneOption(this) } }
            }

            /**
             * Sets the time zone.
             *
             * Example: `Europe/Berlin`
             */
            fun timeZoneId(init: (OperatingSystemImage) -> String) {
                customizationOption { init(it).run { TimeZoneOption(this) } }
            }

            /**
             * This command performs a touch(1)-like operation on FILE.
             */
            fun touch(init: (OperatingSystemImage) -> DiskPath) {
                customizationOption { init(it).run { TouchOption(this) } }
            }

            /**
             * Write CONTENT to FILE.
             */
            fun write(init: (OperatingSystemImage) -> Pair<DiskPath, String>) {
                customizationOption { init(it).run { WriteOption(first, second) } }
            }
        }

        override fun BuildContext.build() = ::VirtCustomizeCustomizationOptionsContext { ::customizationOption.evalAll() }
    }
}
