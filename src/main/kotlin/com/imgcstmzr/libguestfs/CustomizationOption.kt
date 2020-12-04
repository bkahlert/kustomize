package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.string.quoted
import java.nio.file.Path
import java.util.Collections
import java.util.TimeZone

open class CustomizationOption(override val name: String, override val arguments: List<String>) : Option(name, arguments) {
    constructor(name: String, argument: String) : this(name, Collections.singletonList(argument))
}

class ChmodOption(permission: String, val file: Path) : CustomizationOption("--chmod", "$permission:${file.serialized}")
class CommandsFromFileOption(val file: Path) : CustomizationOption("--commands-from-file", file.serialized)
class CopyOption(val source: Path, val dest: Path) : CustomizationOption("--copy", "${source.serialized}:${dest.serialized}")
class CopyInOption(localPath: Path, remoteDir: Path) : CustomizationOption("--copy-in", "${localPath.serialized}:${remoteDir.serialized}")
class DeleteOption(path: Path) : CustomizationOption("--delete", path.serialized)
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
) : CustomizationOption("--edit", "${file.serialized}:$perlExpression")

/**
 * Install SCRIPT inside the guest, so that when the guest first boots up, the script runs (as root, late in the boot process).
 *
 * The alternative version --firstBoot-command is the same, but it conveniently wraps the command up in a single line script for you.
 */
class FirstBootOption(val script: String) : CustomizationOption("--firstBoot", script)

/**
 * Run command (and arguments) inside the guest when the guest first boots up (as root, late in the boot process).
 */
class FirstBootCommandOption(val command: String, vararg args: String) : CustomizationOption("--firstBoot-command", listOf(command, *args))

/**
 * Install the named packages (a comma-separated list). These are installed when the guest first boots using the guest’s package manager (eg. apt, yum, etc.) and the guest’s network connection.
 */
class FirstBootInstallOption(vararg packages: String) : CustomizationOption("--firstBoot-install", packages.joinToString(","))
class HostnameOption(val hostname: String) : CustomizationOption("--hostname", hostname)
class MkdirOption(val dir: Path) : CustomizationOption("--mkdir", dir.serialized)
class MoveOption(val source: Path, val dest: Path) : CustomizationOption("--move", "${source.serialized}:${dest.serialized}")
class PasswordOption private constructor(val user: String, val value: String) : CustomizationOption("--password", "$user$value") {
    companion object {
        fun byFile(user: String, file: Path): PasswordOption = PasswordOption(user, "file:${file.serialized}")
        fun byString(user: String, password: String): PasswordOption = PasswordOption(user, "password:$password")
        fun random(user: String): PasswordOption = PasswordOption(user, "random")
        fun disabled(user: String): PasswordOption = PasswordOption(user, "disabled")
        fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
        class LockedPasswordOption(passwordOption: PasswordOption) :
            CustomizationOption("--password", with(passwordOption) { "$user:locked:$value" })
    }
}

class RootPasswordOption private constructor(val value: String) : CustomizationOption("--root-password", value) {
    companion object {
        fun byFile(file: Path): RootPasswordOption = RootPasswordOption("file:${file.serialized}")
        fun byString(password: String): RootPasswordOption = RootPasswordOption("password:$password")
        fun random(user: String): RootPasswordOption = RootPasswordOption("random")
        fun disabled(): RootPasswordOption = RootPasswordOption("disabled")
        fun PasswordOption.locked(): LockedPasswordOption = LockedPasswordOption(this)
        class LockedPasswordOption(passwordOption: PasswordOption) :
            CustomizationOption("--password", with(passwordOption) { "$user:locked:$value" })
    }
}

class SshInjectOption private constructor(val value: String) : CustomizationOption("--ssh-inject", value) {
    constructor(user: String, keyFile: Path) : this("$user:file:${keyFile.serialized.quoted}")
    constructor(user: String, key: String) : this("$user:string:${key.quoted}")
}

class TimezoneOption(val timeZone: TimeZone) : CustomizationOption("--timezone", "${timeZone.id}") {
    constructor(timeZoneId: String) : this(TimeZone.getTimeZone(timeZoneId))
}

class TouchOption(file: Path) : CustomizationOption("--touch", file.serialized)
class WriteOption(val file: Path, val content: String) : CustomizationOption("--write", "$file:$content")
