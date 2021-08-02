package com.bkahlert.kustomize.libguestfs

import com.bkahlert.kustomize.libguestfs.LibguestfsOption.Companion.relativize
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Option.ColorsOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Option.DiskOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Option.QuietOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Option.TraceOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Option.VerboseOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.ChmodOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.CommandsFromFileOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.CopyInOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.CopyOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.DeleteOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.EditOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootCommandOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootInstallOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.HostnameOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.LinkOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.MkdirOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.MoveOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.PasswordOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.RootPasswordOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.SshInjectOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.TimeZoneOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.TouchOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.WriteOption
import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystemImage.Companion.mountRootForDisk
import koodies.callableOnce
import koodies.docker.DockerContainer
import koodies.docker.DockerExec
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.MountOptions
import koodies.docker.asContainerPath
import koodies.exec.CommandLine
import koodies.exec.Executable
import koodies.io.createParentDirectories
import koodies.io.path.asPath
import koodies.io.path.pathString
import koodies.io.text
import koodies.shell.ShellScript
import koodies.shell.ShellScript.ScriptContext
import koodies.text.LineSeparators.lines
import koodies.text.withRandomSuffix
import java.nio.file.Path
import java.util.Collections
import java.util.TimeZone
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.moveTo
import kotlin.io.path.writeLines
import com.bkahlert.kustomize.libguestfs.LibguestfsOption as LibguestfsCommandLineOption

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
    val options: Options,
    val virtCustomizations: List<VirtCustomization>,
    val dockerOptions: DockerRunCommandLine.Options = Options(
        name = DockerContainer.from(COMMAND.withRandomSuffix()),
        autoCleanup = true,
        mounts = MountOptions {
            mountRootForDisk(options.disk) mountAt "/shared"
            options.disk mountAt "/images/disk.img"
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
        *virtCustomizations.map { it.relativize(options.disk) }.flatten().toTypedArray(),
        name = when (virtCustomizations.size) {
            0 -> "0 $COMMAND operations"
            1 -> "1 $COMMAND operation"
            else -> "${virtCustomizations.size} $COMMAND operations"
        }),
) {

    override fun toString(): String = toCommandLine().toString()

    companion object {
        private const val COMMAND = "virt-customize"
    }

    data class Options(
        val colors: Boolean,
        val quiet: Boolean,
        val verbose: Boolean,
        val trace: Boolean,
        val disks: List<Path>,
    ) : List<Option> by (koodies.builder.buildList {
        if (colors) add(ColorsOption)
        if (quiet) add(QuietOption)
        if (verbose) add(VerboseOption)
        if (trace) add(TraceOption)
        disks.forEach { add(DiskOption(it)) }
    }) {
        constructor(
            vararg disks: Path,
            colors: Boolean = true,
            quiet: Boolean = false,
            verbose: Boolean = false,
            trace: Boolean = true,
        ) : this(colors, quiet, verbose, trace, disks.toList())

        val disk: Path = filterIsInstance<DiskOption>().map { it.disk }
            .also { disks -> check(disks.size == 1) { "The $COMMAND command must add exactly one disk. ${disks.size} found: ${disks.joinToString(", ")}." } }
            .single().apply {
                check(exists()) { "Disk $this does no exist." }
                check(isReadable()) { "Disk $this is not readable." }
                check(isWritable()) { "Disk $this is not writable." }
            }
    }

    open class Option(name: String, arguments: List<String>) : LibguestfsCommandLineOption(name, arguments) {
        class DiskOption(override val disk: Path) : Option("--add", listOf(disk.pathString)), LibguestfsCommandLineOption.DiskOption
        object ColorsOption : Option("--colors", emptyList())
        object QuietOption : Option("--quiet", emptyList())
        object VerboseOption : Option("--verbose", emptyList())
        object TraceOption : Option("-x", emptyList())
    }

    open class VirtCustomization(override val name: String, override val arguments: List<String>) : LibguestfsCommandLineOption(name, arguments) {
        constructor(name: String, argument: String) : this(name, Collections.singletonList(argument))

        class ChmodOption(val permission: String, val file: DiskPath) : VirtCustomization("--chmod", "$permission:$file")
        class CommandsFromFileOption(val file: Path) : VirtCustomization("--commands-from-file", file.pathString)
        class CopyOption(val source: DiskPath, val dest: DiskPath) : VirtCustomization("--copy", "$source:$dest")
        class CopyInOption(val localPath: Path, remoteDir: DiskPath) : VirtCustomization("--copy-in", "${localPath.pathString}:$remoteDir")
        class DeleteOption(path: DiskPath) : VirtCustomization("--delete", path.pathString)
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
        ) : VirtCustomization("--edit", "$file:$perlExpression")

        /**
         * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
         *
         * The alternative version --firstboot-command is the same, but it conveniently wraps the command up in a single line script for you.
         */
        class FirstBootOption(val path: Path) : VirtCustomization("--firstboot", path.pathString)

        /**
         * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
         */
        class FirstBootCommandOption(val command: String) : VirtCustomization("--firstboot-command", command)

        /**
         * Install the named packages (a comma-separated list). These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.) and the guest’s network connection.
         */
        class FirstBootInstallOption(val packages: List<String>) : VirtCustomization("--firstboot-install", packages.joinToString(",")) {
            constructor(vararg packages: String) : this(packages.toList())
        }

        class HostnameOption(val hostname: String) : VirtCustomization("--hostname", hostname)
        class LinkOption(val link: DiskPath, target: DiskPath) : VirtCustomization("--link", "$target:$link")
        class MkdirOption(val dir: DiskPath) : VirtCustomization("--mkdir", dir.toString())
        class MoveOption(val source: DiskPath, val dest: DiskPath) : VirtCustomization("--move", "$source:$dest")
        class PasswordOption private constructor(val user: String, val value: String) : VirtCustomization("--password", "$user:$value") {
            companion object {
                fun byFile(user: String, file: Path): PasswordOption = PasswordOption(user, "file:${file.pathString}")
                fun byString(user: String, password: String): PasswordOption = PasswordOption(user, "password:$password")
                fun random(user: String): PasswordOption = PasswordOption(user, "random")
                fun disabled(user: String): PasswordOption = PasswordOption(user, "disabled")
                fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
                class LockedPasswordOption(passwordOption: PasswordOption) :
                    VirtCustomization("--password", with(passwordOption) { "$user:locked:$value" })
            }
        }

        class RootPasswordOption private constructor(val value: String) : VirtCustomization("--root-password", value) {
            companion object {
                fun byFile(file: Path): RootPasswordOption = RootPasswordOption("file:${file.pathString}")
                fun byString(password: String): RootPasswordOption = RootPasswordOption("password:$password")
                fun random(): RootPasswordOption = RootPasswordOption("random")
                fun disabled(): RootPasswordOption = RootPasswordOption("disabled")
                fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
                class LockedPasswordOption(passwordOption: PasswordOption) :
                    VirtCustomization("--password", with(passwordOption) { "$user:locked:$value" })
            }
        }

        class SshInjectOption private constructor(val value: String) : VirtCustomization("--ssh-inject", value) {
            constructor(user: String, keyFile: Path) : this("$user:file:${keyFile.pathString}")
            constructor(user: String, key: String) : this("$user:string:$key")
        }

        class TimeZoneOption(val timeZone: TimeZone) : VirtCustomization("--timezone", timeZone.id) {
            constructor(timeZoneId: String) : this(TimeZone.getTimeZone(timeZoneId))
        }

        class TouchOption(file: DiskPath) : VirtCustomization("--touch", file.pathString)
        class WriteOption(val file: DiskPath, val content: String) : VirtCustomization("--write", "$file:$content")
    }

    class VirtCustomizationsBuilder(private val osImage: OperatingSystemImage) {

        fun build(init: VirtCustomizationsContext.() -> Unit): List<VirtCustomization> =
            mutableListOf<VirtCustomization>().also { VirtCustomizationsContext(it).init() }

        @VirtCustomizeDsl
        inner class VirtCustomizationsContext(private val virtCustomizations: MutableList<VirtCustomization>) {

            private val fixFirstBootOrder by callableOnce {
                copyIn(FirstBootOrderFix.FIRSTBOOT_FIX, FirstBootOrderFix.text)
                chmods { "0755" to FirstBootOrderFix.FIRSTBOOT_FIX }
            }

            /**
             * Change the permissions of FILE to PERMISSIONS.
             *
             * *Note:* PERMISSIONS by default would be decimal, unless you prefix it with 0 to get octal, ie. use 0700 not 700.
             */
            fun chmods(init: (OperatingSystemImage) -> Pair<String, DiskPath>) {
                virtCustomizations.add(init(osImage).run { ChmodOption(first, second) })
            }

            /**
             * Read the customize commands from a file, one (and its arguments) each line.
             *
             * Each line contains a single customization command and its arguments
             */
            fun commandsFromFiles(init: (OperatingSystemImage) -> Path) {
                virtCustomizations.add(init(osImage).run { CommandsFromFileOption(this) })
            }

            /**
             * Copy files or directories recursively inside the guest.
             *
             * Wildcards cannot be used.
             */
            fun copy(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) {
                virtCustomizations.add(init(osImage).run { CopyOption(first, second) })
            }

            /**
             * Copy local files or directories recursively into the disk image, placing them in the directory REMOTEDIR
             * (which must exist).
             *
             * Wildcards cannot be used.
             */
            fun copyIn(init: (OperatingSystemImage) -> Pair<Path, DiskPath>) {
                virtCustomizations.add(init(osImage).run { MkdirOption(second) })
                virtCustomizations.add(init(osImage).run { CopyInOption(first, second) })
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
                copyIn(diskPath) { createParentDirectories().writeLines(text.lines()) }
            }

            /**
             * Delete a file from the guest. Or delete a directory (and all its contents, recursively).
             */
            fun delete(init: (OperatingSystemImage) -> DiskPath) {
                virtCustomizations.add(init(osImage).run { DeleteOption(this) })
            }

            /**
             * Edit FILE using the Perl expression EXPR.
             */
            fun edit(init: (OperatingSystemImage) -> Pair<DiskPath, String>) {
                virtCustomizations.add(init(osImage).run { EditOption(first, second) })
            }

            /**
             * Set the hostname of the guest to HOSTNAME. You can use a dotted hostname.domainname (FQDN) if you want.
             */
            fun hostname(init: (OperatingSystemImage) -> String) {
                virtCustomizations.add(init(osImage).run { HostnameOption(this) })
            }

            /**
             * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
             */
            fun firstBoot(shellScript: ShellScript) {
                fixFirstBootOrder()
                virtCustomizations.add(run {
                    val scriptFile = shellScript.toFile(echoName = true)
                    val diskPath = LinuxRoot / scriptFile.fileName.pathString
                    val hostPath = osImage.hostPath(diskPath).also { scriptFile.moveTo(it) }
                    FirstBootOption(hostPath)
                })
            }

            /**
             * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
             */
            fun firstBoot(name: String? = null, init: ScriptContext.() -> CharSequence) {
                firstBoot(ShellScript(name, init))
            }

            /**
             * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
             */
            fun firstBootCommand(init: (OperatingSystemImage) -> String) {
                fixFirstBootOrder()
                virtCustomizations.add(init(osImage).run { FirstBootCommandOption(this) })
            }

            /**
             * Install the named packages (a comma-separated list).
             * These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.)
             * and the guest’s network connection.
             */

            fun firstBootInstall(init: (OperatingSystemImage) -> List<String>) {
                fixFirstBootOrder()
                virtCustomizations.add(init(osImage).run { FirstBootInstallOption(this) })
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
                firstBoot("Shutdown") { ShellScript { shutdown }.toString() }
            }

            /**
             * Create a directory in the guest.
             *
             * This uses mkdir -p so any intermediate directories are created, and it also works if the directory already exists.
             */
            fun link(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) {
                virtCustomizations.add(init(osImage).run { LinkOption(first, second) })
            }

            /**
             * Create a directory in the guest.
             *
             * This uses mkdir -p so any intermediate directories are created, and it also works if the directory already exists.
             */
            fun mkdir(init: (OperatingSystemImage) -> DiskPath) {
                virtCustomizations.add(init(osImage).run { MkdirOption(this) })
            }

            /**
             * Move files or directories inside the guest.
             *
             * Wildcards cannot be used.
             */
            fun move(init: (OperatingSystemImage) -> Pair<DiskPath, DiskPath>) {
                virtCustomizations.add(init(osImage).run { MoveOption(first, second) })
            }

            /**
             * Set the password for USER. (Note this option does not create the user account).
             */

            fun password(passwordOption: PasswordOption) {
                virtCustomizations.add(passwordOption)
            }

            /**
             * Set the root password.
             */
            fun rootPassword(rootPasswordOption: RootPasswordOption) {
                virtCustomizations.add(rootPasswordOption)
            }

            /**
             * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
             *
             * The USER must exist already in the guest.
             */
            fun sshInjectFile(init: (OperatingSystemImage) -> Pair<String, Path>) {
                virtCustomizations.add(init(osImage).run { SshInjectOption(first, second) })
            }

            /**
             * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
             *
             * The USER must exist already in the guest.
             */

            fun sshInject(init: (OperatingSystemImage) -> Pair<String, String>) {
                virtCustomizations.add(init(osImage).run { SshInjectOption(first, second) })
            }

            /**
             * Sets the time zone.
             *
             * Example: `Europe/Berlin`
             */
            fun timeZone(init: (OperatingSystemImage) -> TimeZone) {
                virtCustomizations.add(init(osImage).run { TimeZoneOption(this) })
            }

            /**
             * Sets the time zone.
             *
             * Example: `Europe/Berlin`
             */
            fun timeZoneId(init: (OperatingSystemImage) -> String) {
                virtCustomizations.add(init(osImage).run { TimeZoneOption(this) })
            }

            /**
             * This command performs a touch(1)-like operation on FILE.
             */
            fun touch(init: (OperatingSystemImage) -> DiskPath) {
                virtCustomizations.add(init(osImage).run { TouchOption(this) })
            }

            /**
             * Write CONTENT to FILE.
             */
            fun write(init: (OperatingSystemImage) -> Pair<DiskPath, String>) {
                virtCustomizations.add(init(osImage).run { WriteOption(first, second) })
            }
        }
    }
}
