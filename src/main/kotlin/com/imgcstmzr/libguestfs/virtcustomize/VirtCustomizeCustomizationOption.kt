package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.Option
import koodies.io.path.asString
import java.nio.file.Path
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
    class AppendLineOption(val file: Path, val line: String) : VirtCustomizeCustomizationOption("--append-line", "${file.asString()}:$line")
    class ChmodOption(permission: String, val file: Path) : VirtCustomizeCustomizationOption("--chmod", "$permission:${file.asString()}")
    class CommandsFromFileOption(val file: Path) : VirtCustomizeCustomizationOption("--commands-from-file", file.asString())
    class CopyOption(val source: Path, val dest: Path) : VirtCustomizeCustomizationOption("--copy", "${source.asString()}:${dest.asString()}")
    class CopyInOption(localPath: Path, remoteDir: Path) : VirtCustomizeCustomizationOption("--copy-in", "${localPath.asString()}:${remoteDir.asString()}")
    class DeleteOption(path: Path) : VirtCustomizeCustomizationOption("--delete", path.asString())
    class EditOption(
        val file: Path,
        /**
         * The value for the `-e` parameter of a Perl `sed` call.
         *
         * Example: `s/^root:.*?:/root::/`
         *
         * @see <a href="https://libguestfs.org/virt-edit.1.html#non-interactive-editing">Non-Interactive Editing</a>
         */
        perlExpression: String,
    ) : VirtCustomizeCustomizationOption("--edit", "${file.asString()}:$perlExpression")

    /**
     * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
     *
     * The alternative version --firstboot-command is the same, but it conveniently wraps the command up in a single line script for you.
     */
    class FirstBootOption(val path: Path) : VirtCustomizeCustomizationOption("--firstboot", path.asString())

    /**
     * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
     */
    class FirstBootCommandOption(val command: String) : VirtCustomizeCustomizationOption("--firstboot-command", "$command")

    /**
     * Install the named packages (a comma-separated list). These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.) and the guest’s network connection.
     */
    class FirstBootInstallOption(packages: List<String>) : VirtCustomizeCustomizationOption("--firstboot-install", packages.joinToString(",")) {
        constructor(vararg packages: String) : this(packages.toList())
    }

    class HostnameOption(val hostname: String) : VirtCustomizeCustomizationOption("--hostname", hostname)
    class MkdirOption(val dir: Path) : VirtCustomizeCustomizationOption("--mkdir", dir.asString())
    class MoveOption(val source: Path, val dest: Path) : VirtCustomizeCustomizationOption("--move", "${source.asString()}:${dest.asString()}")
    class PasswordOption private constructor(val user: String, val value: String) : VirtCustomizeCustomizationOption("--password", "$user:$value") {
        companion object {
            fun byFile(user: String, file: Path): PasswordOption = PasswordOption(user, "file:${file.asString()}")
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
            fun byFile(file: Path): RootPasswordOption = RootPasswordOption("file:${file.asString()}")
            fun byString(password: String): RootPasswordOption = RootPasswordOption("password:$password")
            fun random(user: String): RootPasswordOption = RootPasswordOption("random")
            fun disabled(): RootPasswordOption = RootPasswordOption("disabled")
            fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
            class LockedPasswordOption(passwordOption: PasswordOption) :
                VirtCustomizeCustomizationOption("--password", with(passwordOption) { "$user:locked:$value" })
        }
    }

    class SshInjectOption private constructor(val value: String) : VirtCustomizeCustomizationOption("--ssh-inject", value) {
        constructor(user: String, keyFile: Path) : this("$user:file:${keyFile.asString()}")
        constructor(user: String, key: String) : this("$user:string:$key")
    }

    class TimezoneOption(val timeZone: TimeZone) : VirtCustomizeCustomizationOption("--timezone", timeZone.id) {
        constructor(timeZoneId: String) : this(TimeZone.getTimeZone(timeZoneId))
    }

    class TouchOption(file: Path) : VirtCustomizeCustomizationOption("--touch", file.asString())
    class WriteOption(val file: Path, val content: String) : VirtCustomizeCustomizationOption("--write", "$file:$content")

}
