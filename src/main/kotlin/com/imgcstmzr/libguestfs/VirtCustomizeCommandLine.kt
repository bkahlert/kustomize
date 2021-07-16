package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.FirstBootWait.waitForFirstBootToComplete
import com.imgcstmzr.libguestfs.LibguestfsOption.Companion.relativize
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Companion.VirtCustomizeCommandLineContext
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.AppendLineOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.ChmodOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.CommandsFromFileOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.CopyInOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.CopyOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.DeleteOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.EditOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootCommandOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootInstallOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.HostnameOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.LinkOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.MkdirOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.MoveOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.PasswordOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.RootPasswordOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.SshInjectOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.TimeZoneOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.TouchOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.WriteOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Option.ColorsOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Option.DiskOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Option.QuietOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Option.TraceOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Option.VerboseOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.OptionsBuilder.VirtCustomizeOptionsContext
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemImage.Companion.mountRootForDisk
import com.imgcstmzr.os.asExtra
import koodies.builder.BooleanBuilder
import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.OnOff.Context
import koodies.builder.BuilderTemplate
import koodies.builder.SkippableBuilder
import koodies.builder.build
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.callableOnce
import koodies.collections.head
import koodies.collections.tail
import koodies.docker.DockerContainer
import koodies.docker.DockerExec
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.MountOptions
import koodies.docker.asContainerPath
import koodies.exec.CommandLine
import koodies.exec.Executable
import koodies.io.path.asPath
import koodies.io.path.pathString
import koodies.io.path.withDirectoriesCreated
import koodies.io.text
import koodies.shell.ShellScript
import koodies.shell.ShellScript.ScriptContext
import koodies.text.LineSeparators.lines
import koodies.text.truncate
import koodies.text.truncateTo
import koodies.text.withRandomSuffix
import java.nio.file.Path
import java.util.Collections
import java.util.TimeZone
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.writeLines
import com.imgcstmzr.libguestfs.LibguestfsOption as LibguestfsCommandLineOption

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
    val options: List<Option>,
    val customizations: List<Customization>,
    val disk: Path = options.filterIsInstance<DiskOption>().map { it.disk }
        .also { disks -> check(disks.size == 1) { "The $COMMAND command must add exactly one disk. ${disks.size} found: ${disks.joinToString(", ")}." } }
        .single().apply {
            check(exists()) { "Disk $this does no exist." }
            check(isReadable()) { "Disk $this is not readable." }
            check(isWritable()) { "Disk $this is not writable." }
        },
    val dockerOptions: Options = Options(
        entryPoint = COMMAND,
        name = DockerContainer.from(COMMAND.withRandomSuffix()),
        autoCleanup = true,
        mounts = MountOptions {
            mountRootForDisk(disk) mountAt "/shared"
            disk mountAt "/images/disk.img"
        },
        workingDirectory = "/shared".asContainerPath(),
    ),
) : Executable<DockerExec> by DockerRunCommandLine(
    LibguestfsImage,
    dockerOptions,
    CommandLine(
        COMMAND,
        *DiskOption("/images/disk.img".asPath()).toTypedArray(),
        *options.filter { it !is DiskOption }.flatten().toTypedArray(),
        *customizations.map { it.relativize(disk) }.flatten().toTypedArray(),
        name = customizations
            .map { option -> "${option.arguments.head}(${option.arguments.tail.joinToString(", ")})" }
            .run { map { it.truncateTo(25).truncate(25).toString() } }
            .asExtra()),
) {

    override fun toString(): String = toCommandLine().toString()

    // TODO remove builder
    companion object : BuilderTemplate<VirtCustomizeCommandLineContext, (OperatingSystemImage) -> VirtCustomizeCommandLine>() {

        private const val COMMAND = "virt-customize"

        @VirtCustomizeDsl
        class VirtCustomizeCommandLineContext(override val captures: CapturesMap) : CapturingContext() {
            val options: SkippableCapturingBuilderInterface<VirtCustomizeOptionsContext.() -> Unit, List<(OperatingSystemImage) -> Option>?> by OptionsBuilder
            val customizations: SkippableCapturingBuilderInterface<CustomizationsContext.() -> Unit, List<(OperatingSystemImage) -> Customization>?> by CustomizationsBuilder
        }

        override fun BuildContext.build(): (OperatingSystemImage) -> VirtCustomizeCommandLine = ::VirtCustomizeCommandLineContext {
            { osImage: OperatingSystemImage ->
                val options: List<(OperatingSystemImage) -> Option> = ::options.eval()
                val customizations: List<(OperatingSystemImage) -> Customization> = ::customizations.eval()
                VirtCustomizeCommandLine(options.map { it(osImage) }, customizations.map { it(osImage) })
            }
        }

        @VirtCustomizeDsl
        fun build(osImage: OperatingSystemImage, init: VirtCustomizeCommandLineContext.() -> Unit): VirtCustomizeCommandLine = build(init)(osImage)
    }

    open class Option(name: String, arguments: List<String>) : LibguestfsCommandLineOption(name, arguments) {

        class DiskOption(override val disk: Path) : Option("--add", listOf(disk.pathString)), LibguestfsCommandLineOption.DiskOption
        class ColorsOption : Option("--colors", emptyList())
        class QuietOption : Option("--quiet", emptyList())
        object VerboseOption : Option("--verbose", emptyList())
        object TraceOption : Option("-x", emptyList())
    }

    object OptionsBuilder : BuilderTemplate<VirtCustomizeOptionsContext, List<(OperatingSystemImage) -> Option>>() {

        @VirtCustomizeDsl
        class VirtCustomizeOptionsContext(override val captures: CapturesMap) : CapturingContext() {
            val option: ((OperatingSystemImage) -> Option) -> Unit by function<(OperatingSystemImage) -> Option>()

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
            val colors: SkippableBuilder<Context.() -> BooleanValue, Boolean, Unit> by BooleanBuilder.OnOff then {
                if (it) option { ColorsOption() }
            }

            /**
             * Don’t print log messages.
             *
             * To enable detailed logging of individual file operations, use -x.
             */
            val quiet: SkippableBuilder<Context.() -> BooleanValue, Boolean, Unit> by BooleanBuilder.OnOff then {
                if (it) option { QuietOption() }
            }

            /**
             * Display version number and exit.
             */
            val verbose: SkippableBuilder<Context.() -> BooleanValue, Boolean, Unit> by BooleanBuilder.OnOff then {
                if (it) option { VerboseOption }
            }

            /**
             * Enable tracing of libguestfs API calls.
             */
            val trace: SkippableBuilder<Context.() -> BooleanValue, Boolean, Unit> by BooleanBuilder.OnOff then {
                if (it) option { TraceOption }
            }
        }

        override fun BuildContext.build(): List<(OperatingSystemImage) -> Option> = ::VirtCustomizeOptionsContext { evalAll() }
    }

    open class Customization(override val name: String, override val arguments: List<String>) :
        com.imgcstmzr.libguestfs.LibguestfsOption(name, arguments) {
        constructor(name: String, argument: String) : this(name, Collections.singletonList(argument))

        /**
         * Append a single line of text to the [file].
         *
         * If the file does not already end with a newline, then one is added before the appended line.
         * Also a newline is added to the end of the [line] string automatically.
         */
        class AppendLineOption(val file: DiskPath, val line: String) : Customization("--append-line", "$file:$line")
        class ChmodOption(val permission: String, val file: DiskPath) : Customization("--chmod", "$permission:$file")
        class CommandsFromFileOption(val file: Path) : Customization("--commands-from-file", file.pathString)
        class CopyOption(val source: DiskPath, val dest: DiskPath) : Customization("--copy", "$source:$dest")
        class CopyInOption(val localPath: Path, remoteDir: DiskPath) : Customization("--copy-in", "${localPath.pathString}:$remoteDir")
        class DeleteOption(path: DiskPath) : Customization("--delete", path.pathString)
        class EditOption(
            val file: DiskPath,
            /**
             * The value for the `-e` parameter of a Perl `sed` call.
             *
             * Example: `s/^root:.*?:/root::/`
             *
             * @see <a href="https://libguestfs.org/virt-edit.1.html#non-interactive-editing">Non-Interactive Editing</a>
             */
            perlExpression: String,
        ) : Customization("--edit", "$file:$perlExpression")

        /**
         * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
         *
         * The alternative version --firstboot-command is the same, but it conveniently wraps the command up in a single line script for you.
         */
        class FirstBootOption(val path: Path) : Customization("--firstboot", path.pathString)

        /**
         * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
         */
        class FirstBootCommandOption(val command: String) : Customization("--firstboot-command", command)

        /**
         * Install the named packages (a comma-separated list). These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.) and the guest’s network connection.
         */
        class FirstBootInstallOption(val packages: List<String>) : Customization("--firstboot-install", packages.joinToString(",")) {
            constructor(vararg packages: String) : this(packages.toList())
        }

        class HostnameOption(val hostname: String) : Customization("--hostname", hostname)
        class LinkOption(val link: DiskPath, target: DiskPath) : Customization("--link", "$target:$link")
        class MkdirOption(val dir: DiskPath) : Customization("--mkdir", dir.toString())
        class MoveOption(val source: DiskPath, val dest: DiskPath) : Customization("--move", "$source:$dest")
        class PasswordOption private constructor(val user: String, val value: String) : Customization("--password", "$user:$value") {
            companion object {
                fun byFile(user: String, file: Path): PasswordOption = PasswordOption(user, "file:${file.pathString}")
                fun byString(user: String, password: String): PasswordOption = PasswordOption(user, "password:$password")
                fun random(user: String): PasswordOption = PasswordOption(user, "random")
                fun disabled(user: String): PasswordOption = PasswordOption(user, "disabled")
                fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
                class LockedPasswordOption(passwordOption: PasswordOption) :
                    Customization("--password", with(passwordOption) { "$user:locked:$value" })
            }
        }

        class RootPasswordOption private constructor(val value: String) : Customization("--root-password", value) {
            companion object {
                fun byFile(file: Path): RootPasswordOption = RootPasswordOption("file:${file.pathString}")
                fun byString(password: String): RootPasswordOption = RootPasswordOption("password:$password")
                fun random(): RootPasswordOption = RootPasswordOption("random")
                fun disabled(): RootPasswordOption = RootPasswordOption("disabled")
                fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
                class LockedPasswordOption(passwordOption: PasswordOption) :
                    Customization("--password", with(passwordOption) { "$user:locked:$value" })
            }
        }

        class SshInjectOption private constructor(val value: String) : Customization("--ssh-inject", value) {
            constructor(user: String, keyFile: Path) : this("$user:file:${keyFile.pathString}")
            constructor(user: String, key: String) : this("$user:string:$key")
        }

        class TimeZoneOption(val timeZone: TimeZone) : Customization("--timezone", timeZone.id) {
            constructor(timeZoneId: String) : this(TimeZone.getTimeZone(timeZoneId))
        }

        class TouchOption(file: DiskPath) : Customization("--touch", file.pathString)
        class WriteOption(val file: DiskPath, val content: String) : Customization("--write", "$file:$content")
    }

    object CustomizationsBuilder : BuilderTemplate<CustomizationsContext, List<(OperatingSystemImage) -> Customization>>() {

        @VirtCustomizeDsl
        class CustomizationsContext(override val captures: CapturesMap) : CapturingContext() {

            private val fixFirstBootOrder by callableOnce {
                copyIn(FirstBootOrderFix.FIRSTBOOT_FIX, FirstBootOrderFix.text)
                chmods { "0755" to FirstBootOrderFix.FIRSTBOOT_FIX }
            }

            private val waitForFirstBoot by callableOnce { waitForFirstBootToComplete() }

            val customizationOption: ((OperatingSystemImage) -> Customization) -> Unit by function<(OperatingSystemImage) -> Customization>()

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
            fun commandsFromFiles(init: (OperatingSystemImage) -> Path) {
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
            fun copyIn(init: (OperatingSystemImage) -> Pair<Path, DiskPath>) {
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
            fun copyIn(diskPath: DiskPath, init: Path.(OperatingSystemImage) -> Unit = {}) {
                copyIn { osImage ->
                    require(diskPath.isAbsolute) { "$diskPath must be absolute but is not." }
                    val hostPath = osImage.hostPath(diskPath)
                    hostPath.init(osImage)
                    hostPath to (diskPath.parentOrNull ?: diskPath)
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
                waitForFirstBoot()
                customizationOption { osImage: OperatingSystemImage ->
                    val diskPath = LinuxRoot / "script".withRandomSuffix().plus(".sh")
                    val hostPath = osImage.hostPath(diskPath)
                    val scriptFile = shellScript.toFile(hostPath)
                    FirstBootOption(scriptFile)
                }
            }

            /**
             * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
             */
            fun firstBoot(name: String? = null, init: ScriptContext.(OperatingSystemImage) -> CharSequence) {
                fixFirstBootOrder()
                waitForFirstBoot()
                customizationOption { osImage: OperatingSystemImage ->
                    val diskPath = LinuxRoot / "script".withRandomSuffix().plus(".sh")
                    val hostPath = osImage.hostPath(diskPath)
                    val scriptFile = ShellScript(name) { init(osImage) }.toFile(hostPath)
                    FirstBootOption(scriptFile)
                }
            }

            /**
             * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
             */
            fun firstBootCommand(init: (OperatingSystemImage) -> String) {
                fixFirstBootOrder()
                waitForFirstBoot()
                customizationOption { init(it).run { FirstBootCommandOption(this) } }
            }

            /**
             * Install the named packages (a comma-separated list).
             * These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.)
             * and the guest’s network connection.
             */

            fun firstBootInstall(init: (OperatingSystemImage) -> List<String>) {
                fixFirstBootOrder()
                waitForFirstBoot()
                customizationOption { init(it).run { FirstBootInstallOption(this) } }
            }

            /**
             * Run [OperatingSystemImage] specific [OperatingSystemImage.shutdownCommand]
             * when the guest first boots up (as root, late in the boot process).
             *
             * This command can be used to shutdown a machine after a couple
             * previously added first boot commands have finished.
             *
             * Actually it must be called if no other mechanism shuts down the machine
             * to avoid the image customization to hang.
             *
             * @see firstBoot
             * @see firstBootCommand
             * @see firstBootInstall
             */
            fun firstBootShutdownCommand() {
                firstBoot("Shutdown") { it.shutdownCommand.shellCommand }
            }

            /**
             * Create a directory in the guest.
             *
             * This uses mkdir -p so any intermediate directories are created, and it also works if the directory already exists.
             */
            fun link(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) {
                customizationOption { init(it).run { LinkOption(first, second) } }
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
            fun sshInjectFile(init: (OperatingSystemImage) -> Pair<String, Path>) {
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

        override fun BuildContext.build(): List<(OperatingSystemImage) -> Customization> =
            ::CustomizationsContext { ::customizationOption.evalAll() }
    }
}
