package com.imgcstmzr.libguestfs.virtcustomize

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.builder.ListBuilderInit
import com.bkahlert.koodies.builder.MapBuilderInit
import com.bkahlert.koodies.builder.OnOffBuilderInit
import com.bkahlert.koodies.builder.PairBuilderInit
import com.bkahlert.koodies.builder.buildListTo
import com.bkahlert.koodies.builder.buildMapTo
import com.bkahlert.koodies.builder.buildTo
import com.bkahlert.koodies.docker.DockerRunAdaptable
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.libguestfs.docker.VirtCustomizeDockerAdaptable
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
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.TimezoneOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.TouchOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.WriteOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.ColorsOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.DiskOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.QuietOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.TraceOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.VerboseOption
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.log.RenderingLogger
import java.nio.file.Path
import java.util.TimeZone

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
class VirtCustomizeCommandLine(val options: List<VirtCustomizeOption>, val customizationOptions: List<VirtCustomizeCustomizationOption>) :
    DockerRunAdaptable by VirtCustomizeDockerAdaptable(options, customizationOptions),
    LibguestfsCommandLine(command = COMMAND, arguments = options.flatten() + customizationOptions.flatten()) {

    companion object {
        const val COMMAND = "virt-customize"

        fun RenderingLogger.customize(operatingSystemImage: OperatingSystemImage, init: VirtCustomizeCustomizationOptionsBuilder.() -> Unit): Int =
            VirtCustomizeCommandLineBuilder.build {
                options {
                    colors { on }
                    trace { on }
                    disks { +operatingSystemImage.file }
                }
                customizationOptions(init)
            }.execute(this)
    }

    @VirtCustomizeDsl
    class VirtCustomizeCommandLineBuilder(
        private val options: MutableList<VirtCustomizeOption> = mutableListOf(),
        private val customizationOptions: MutableList<VirtCustomizeCustomizationOption> = mutableListOf(),
    ) {
        companion object {
            fun build(init: VirtCustomizeCommandLineBuilder.() -> Unit): VirtCustomizeCommandLine =
                VirtCustomizeCommandLineBuilder().apply(init).let { VirtCustomizeCommandLine(it.options, it.customizationOptions) }
        }

        fun options(init: VirtCustomizeOptionsBuilder.() -> Unit) {
            init.buildListTo(options)
        }

        fun customizationOptions(init: VirtCustomizeCustomizationOptionsBuilder.() -> Unit) {
            init.buildListTo(customizationOptions)
        }
    }

    @VirtCustomizeDsl
    class VirtCustomizeOptionsBuilder : ListBuilder<VirtCustomizeOption>() {

        /**
         * Add file which should be a disk image from a virtual machine.
         *
         * The format of the disk image is auto-detected.
         */
        fun disks(init: ListBuilderInit<Path>) =
            init.buildListTo(list) { DiskOption(this) }

        /**
         * Use ANSI colour sequences to colourize messages.
         * This is the default when the output is a tty.
         *
         * If the output of the program is redirected to a file, ANSI colour sequences are disabled unless you use this option.
         */
        fun colors(init: OnOffBuilderInit) =
            init.buildTo(list) { ColorsOption() }

        /**
         * Don’t print log messages.
         *
         * To enable detailed logging of individual file operations, use -x.
         */
        fun quiet(init: OnOffBuilderInit) =
            init.buildTo(list) { QuietOption() }

        /**
         * Display version number and exit.
         */
        fun verbose(init: OnOffBuilderInit) =
            init.buildTo(list) { VerboseOption() }

        /**
         * Enable tracing of libguestfs API calls.
         */
        fun trace(init: OnOffBuilderInit) =
            init.buildTo(list) { TraceOption() }
    }

    @VirtCustomizeDsl
    class VirtCustomizeCustomizationOptionsBuilder : ListBuilder<VirtCustomizeCustomizationOption>() {

        fun appendLine(init: () -> AppendLineOption) =
            init.buildTo(list)

        /**
         * Change the permissions of FILE to PERMISSIONS.
         *
         * *Note:* PERMISSIONS by default would be decimal, unless you prefix it with 0 to get octal, ie. use 0700 not 700.
         */
        fun chmods(init: ListBuilderInit<ChmodOption>) =
            init.buildListTo(list)

        /**
         * Read the customize commands from a file, one (and its arguments) each line.
         *
         * Each line contains a single customization command and its arguments
         */
        fun commandsFromFiles(init: ListBuilderInit<Path>) =
            init.buildListTo(list) { CommandsFromFileOption(this) }

        /**
         * Copy files or directories recursively inside the guest.
         *
         * Wildcards cannot be used.
         */
        fun copy(init: MapBuilderInit<Path, Path>) =
            init.buildMapTo(list) { CopyOption(key, value) }

        /**
         * Copy local files or directories recursively into the disk image, placing them in the directory REMOTEDIR
         * (which must exist).
         *
         * Wildcards cannot be used.
         */
        fun copyIn(init: MapBuilderInit<Path, Path>) =
            init.buildMapTo(list) { CopyInOption(key, value) }

        /**
         * Delete a file from the guest. Or delete a directory (and all its contents, recursively).
         */
        fun delete(init: ListBuilderInit<Path>) =
            init.buildListTo(list) { DeleteOption(this) }

        /**
         * Edit FILE using the Perl expression EXPR.
         */
        fun edit(init: MapBuilderInit<Path, String>) =
            init.buildMapTo(list) { EditOption(key, value) }

        /**
         * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
         */
        fun firstBoot(init: ListBuilderInit<String>) =
            init.buildListTo(list) { FirstBootOption(this) }

        /**
         * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
         */
        fun firstBootCommand(init: ListBuilderInit<String>) =
            init.buildListTo(list) { FirstBootCommandOption(this) }

        /**
         * Install the named packages (a comma-separated list).
         * These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.)
         * and the guest’s network connection.
         */
        fun firstBootInstall(init: ListBuilderInit<String>) =
            init.buildListTo(list) { FirstBootInstallOption(this) }

        /**
         * Set the hostname of the guest to HOSTNAME. You can use a dotted hostname.domainname (FQDN) if you want.
         */
        fun hostname(init: () -> String) =
            init.buildTo(list) { HostnameOption(this) }

        /**
         * Create a directory in the guest.
         *
         * This uses mkdir -p so any intermediate directories are created, and it also works if the directory already exists.
         */
        fun mkdir(init: () -> Path) =
            init.buildTo(list) { MkdirOption(this) }

        /**
         * Move files or directories inside the guest.
         *
         * Wildcards cannot be used.
         */
        fun move(init: PairBuilderInit<Path, Path>) =
            init.buildTo(list) { MoveOption(first, second) }

        /**
         * Set the password for USER. (Note this option does not create the user account).
         */
        fun password(init: () -> PasswordOption) =
            init.buildTo(list)

        /**
         * Set the root password.
         */
        fun rootPassword(init: () -> RootPasswordOption) =
            init.buildTo(list)

        /**
         * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
         *
         * The USER must exist already in the guest.
         */
        fun sshInjectFile(init: PairBuilderInit<String, Path>) =
            init.buildTo(list) { SshInjectOption(first, second) }

        /**
         * Inject an ssh key so the given USER will be able to log in over ssh without supplying a password.
         *
         * The USER must exist already in the guest.
         */
        fun sshInject(init: PairBuilderInit<String, String>) =
            init.buildTo(list) { SshInjectOption(first, second) }

        /**
         * Sets the time zone.
         *
         * Example: `Europe/Berlin`
         */
        fun timeZone(init: () -> TimeZone) =
            init.buildTo(list) { TimezoneOption(this) }

        /**
         * Sets the time zone.
         *
         * Example: `Europe/Berlin`
         */
        fun timeZoneId(init: () -> String) =
            init.buildTo(list) { TimezoneOption(this) }

        /**
         * This command performs a touch(1)-like operation on FILE.
         */
        fun touch(init: () -> Path) =
            init.buildTo(list) { TouchOption(this) }

        /**
         * Write CONTENT to FILE.
         */
        fun write(init: PairBuilderInit<Path, String>) =
            init.buildTo(list) { WriteOption(first, second) }
    }
}
