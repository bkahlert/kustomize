package com.imgcstmzr.libguestfs.virtcustomize

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.string.quoted
import com.imgcstmzr.libguestfs.Option
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
    class AppendLineOption(val file: Path, val line: String) : VirtCustomizeCustomizationOption("--append-line", "${file.serialized}:$line")
    class ChmodOption(permission: String, val file: Path) : VirtCustomizeCustomizationOption("--chmod", "$permission:${file.serialized}")
    class CommandsFromFileOption(val file: Path) : VirtCustomizeCustomizationOption("--commands-from-file", file.serialized)
    class CopyOption(val source: Path, val dest: Path) : VirtCustomizeCustomizationOption("--copy", "${source.serialized}:${dest.serialized}")
    class CopyInOption(localPath: Path, remoteDir: Path) : VirtCustomizeCustomizationOption("--copy-in", "${localPath.serialized}:${remoteDir.serialized}")
    class DeleteOption(path: Path) : VirtCustomizeCustomizationOption("--delete", path.serialized)
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
    ) : VirtCustomizeCustomizationOption("--edit", "${file.serialized}:$perlExpression")

    /**
     * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
     *
     * The alternative version --firstBoot-command is the same, but it conveniently wraps the command up in a single line script for you.
     */
    class FirstBootOption(val script: String) : VirtCustomizeCustomizationOption("--firstBoot", script)

    /**
     * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
     */
    class FirstBootCommandOption(val command: String, vararg args: String) : VirtCustomizeCustomizationOption("--firstBoot-command", listOf(command, *args))

    /**
     * Install the named packages (a comma-separated list). These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.) and the guest’s network connection.
     */
    class FirstBootInstallOption(vararg packages: String) : VirtCustomizeCustomizationOption("--firstBoot-install", packages.joinToString(","))
    class HostnameOption(val hostname: String) : VirtCustomizeCustomizationOption("--hostname", hostname)
    class MkdirOption(val dir: Path) : VirtCustomizeCustomizationOption("--mkdir", dir.serialized)
    class MoveOption(val source: Path, val dest: Path) : VirtCustomizeCustomizationOption("--move", "${source.serialized}:${dest.serialized}")
    class PasswordOption private constructor(val user: String, val value: String) : VirtCustomizeCustomizationOption("--password", "$user:$value") {
        companion object {
            fun byFile(user: String, file: Path): PasswordOption = PasswordOption(user, "file:${file.serialized}")
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
            fun byFile(file: Path): RootPasswordOption = RootPasswordOption("file:${file.serialized}")
            fun byString(password: String): RootPasswordOption = RootPasswordOption("password:$password")
            fun random(user: String): RootPasswordOption = RootPasswordOption("random")
            fun disabled(): RootPasswordOption = RootPasswordOption("disabled")
            fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
            class LockedPasswordOption(passwordOption: PasswordOption) :
                VirtCustomizeCustomizationOption("--password", with(passwordOption) { "$user:locked:$value" })
        }
    }

    class SshInjectOption private constructor(val value: String) : VirtCustomizeCustomizationOption("--ssh-inject", value) {
        constructor(user: String, keyFile: Path) : this("$user:file:${keyFile.serialized.quoted}")
        constructor(user: String, key: String) : this("$user:string:${key.quoted}")
    }

    class TimezoneOption(val timeZone: TimeZone) : VirtCustomizeCustomizationOption("--timezone", "${timeZone.id}") {
        constructor(timeZoneId: String) : this(TimeZone.getTimeZone(timeZoneId))
    }

    class TouchOption(file: Path) : VirtCustomizeCustomizationOption("--touch", file.serialized)
    class WriteOption(val file: Path, val content: String) : VirtCustomizeCustomizationOption("--write", "$file:$content")

}
