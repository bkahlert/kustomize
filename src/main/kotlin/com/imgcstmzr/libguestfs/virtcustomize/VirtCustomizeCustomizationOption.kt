package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import com.imgcstmzr.libguestfs.Option
import koodies.io.path.asString
import java.util.Collections
import java.util.TimeZone

sealed class VirtCustomizeCustomizationOption(override val name: String, override val arguments: List<String>) : Option(name, arguments) {
    constructor(name: String, argument: String) : this(name, Collections.singletonList(argument))

    /**
     * Append a single line of text to the [file].
     *
     * If the file does not already end with a newline, then one is added before the appended line.
     * Also a newline is added to the end of the [line] string automatically.
     */
    class AppendLineOption(val file: DiskPath, val line: String) : VirtCustomizeCustomizationOption("--append-line", "$file:$line")
    class ChmodOption(val permission: String, val file: DiskPath) : VirtCustomizeCustomizationOption("--chmod", "$permission:$file")
    class CommandsFromFileOption(val file: HostPath) : VirtCustomizeCustomizationOption("--commands-from-file", file.asString())
    class CopyOption(val source: DiskPath, val dest: DiskPath) : VirtCustomizeCustomizationOption("--copy", "$source:$dest")
    class CopyInOption(val localPath: HostPath, remoteDir: DiskPath) : VirtCustomizeCustomizationOption("--copy-in", "${localPath.asString()}:$remoteDir")
    class DeleteOption(path: DiskPath) : VirtCustomizeCustomizationOption("--delete", path.toString())
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
    ) : VirtCustomizeCustomizationOption("--edit", "$file:$perlExpression")

    /**
     * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
     *
     * The alternative version --firstboot-command is the same, but it conveniently wraps the command up in a single line script for you.
     */
    class FirstBootOption(val path: HostPath) : VirtCustomizeCustomizationOption("--firstboot", path.asString())

    /**
     * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
     */
    class FirstBootCommandOption(val command: String) : VirtCustomizeCustomizationOption("--firstboot-command", command)

    /**
     * Install the named packages (a comma-separated list). These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.) and the guest’s network connection.
     */
    class FirstBootInstallOption(packages: List<String>) : VirtCustomizeCustomizationOption("--firstboot-install", packages.joinToString(",")) {
        constructor(vararg packages: String) : this(packages.toList())
    }

    class HostnameOption(val hostname: String) : VirtCustomizeCustomizationOption("--hostname", hostname)
    class MkdirOption(val dir: DiskPath) : VirtCustomizeCustomizationOption("--mkdir", dir.toString())
    class MoveOption(val source: DiskPath, val dest: DiskPath) : VirtCustomizeCustomizationOption("--move", "$source:$dest")
    class PasswordOption private constructor(val user: String, val value: String) : VirtCustomizeCustomizationOption("--password", "$user:$value") {
        companion object {
            fun byFile(user: String, file: HostPath): PasswordOption = PasswordOption(user, "file:${file.asString()}")
            fun byString(user: String, password: String): PasswordOption = PasswordOption(user, "password:$password")
            fun random(user: String): PasswordOption = PasswordOption(user, "random")
            fun disabled(user: String): PasswordOption = PasswordOption(user, "disabled")
            fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
            class LockedPasswordOption(passwordOption: PasswordOption) :
                VirtCustomizeCustomizationOption("--password", with(passwordOption) { "$user:locked:$value" })
        }
    }

    class RootPasswordOption private constructor(val value: String) : VirtCustomizeCustomizationOption("--root-password", value) {
        companion object {
            fun byFile(file: HostPath): RootPasswordOption = RootPasswordOption("file:${file.asString()}")
            fun byString(password: String): RootPasswordOption = RootPasswordOption("password:$password")
            fun random(): RootPasswordOption = RootPasswordOption("random")
            fun disabled(): RootPasswordOption = RootPasswordOption("disabled")
            fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
            class LockedPasswordOption(passwordOption: PasswordOption) :
                VirtCustomizeCustomizationOption("--password", with(passwordOption) { "$user:locked:$value" })
        }
    }

    class SshInjectOption private constructor(val value: String) : VirtCustomizeCustomizationOption("--ssh-inject", value) {
        constructor(user: String, keyFile: HostPath) : this("$user:file:${keyFile.asString()}")
        constructor(user: String, key: String) : this("$user:string:$key")
    }

    class TimeZoneOption(val timeZone: TimeZone) : VirtCustomizeCustomizationOption("--timezone", timeZone.id) {
        constructor(timeZoneId: String) : this(TimeZone.getTimeZone(timeZoneId))
    }

    class TouchOption(file: DiskPath) : VirtCustomizeCustomizationOption("--touch", file.toString())
    class WriteOption(val file: DiskPath, val content: String) : VirtCustomizeCustomizationOption("--write", "$file:$content")

}
