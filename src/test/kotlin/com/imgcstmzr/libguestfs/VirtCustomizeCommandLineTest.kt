package com.imgcstmzr.libguestfs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import java.nio.file.Path

@Execution(CONCURRENT)
class VirtCustomizeCommandLineTest {

    @Test
    fun `should be instantiatable`() {
        expectCatching { createVirtCustomizeCommand() }.isSuccess()
    }

    @Test
    fun `should break even arguments by default`() {
        expectThat(createVirtCustomizeCommand().toString()).isEqualTo("""
            virt-customize \
            --add \
            my/disk.img \
            --verbose \
            -x \
            --chmod \
            0664:/chmod-file \
            --chmod \
            0664:/other/file \
            --commands-from-file \
            commands/from/files-1 \
            --commands-from-file \
            commands/from/files-2 \
            --copy \
            source:copy \
            --copy-in \
            from/host:to/guest \
            --delete \
            delete/file1 \
            --edit \
            /etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/ \
            --firstBoot \
            "first boot" \
            --firstBoot \
            scripts \
            --firstBoot-command \
            command \
            arg1 \
            arg2 \
            --firstBoot-command \
            boot-command \
            --firstBoot-install \
            package1 \
            --firstBoot-install \
            package2 \
            --firstBoot-install \
            package3 \
            --hostname \
            new-hostname \
            --mkdir \
            new/dir \
            --move \
            new/dir:moved/dir \
            --password \
            "super-adminpassword:super secure" \
            --root-password \
            disabled \
            --ssh-inject \
            "file-user:file:\"file/key\"" \
            --ssh-inject \
            "string-user:string:\"string-key\"" \
            --timezone \
            Europe/Berlin \
            --touch \
            touch/file \
            --write \
            "write/file:write content"
        """.trimIndent())
    }
}

private fun f(path: String): Path = Path.of(path)

internal fun createVirtCustomizeCommand() = VirtCustomizeCommandLine {
    disks { +Path.of("my/disk.img") }
    quiet { off }
    verbose { on }
    trace { on }
    chmods {
        +ChmodOption("0664", f("/chmod-file"))
        +ChmodOption("0664", f("/other/file"))
    }
    commandsFromFiles {
        +f("commands/from/files-1")
        +f("commands/from/files-2")
    }
    copy { f("source") to f("copy") }
    copyIn { f("from/host") to f("to/guest") }
    delete { +f("delete/file1") to f("/delete/dir/2") }
    edit { f("/etc/dnf/dnf.conf") to "s/gpgcheck=1/gpgcheck=0/" }
    firstBoot {
        +"first boot"
        +"scripts"
    }
    firstBootCommand {
        +("command" to arrayOf("arg1", "arg2"))
        +("boot-command" to emptyArray<String>())
    }
    firstBootInstall {
        +listOf("package1", "package2")
        +listOf("package3")
    }
    hostname { "new-hostname" }
    mkdir { f("new/dir") }
    move { f("new/dir") to f("moved/dir") }
    password { PasswordOption.byString("super-admin", "super secure") }
    rootPassword { RootPasswordOption.disabled() }
    sshInjectFile { "file-user" to f("file/key") }
    sshInject { "string-user" to "string-key" }
    timeZoneId { "Europe/Berlin" }
    touch { f("touch/file") }
    write { f("write/file") to "write content" }
}
