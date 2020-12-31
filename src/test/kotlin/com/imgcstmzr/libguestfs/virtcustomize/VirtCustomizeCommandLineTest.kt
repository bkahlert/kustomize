package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.SharedPath
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.libguestfs.resolveOnDocker
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootOption
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.matchesCurlyPattern
import koodies.io.path.asString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectCatching
import strikt.assertions.all
import strikt.assertions.filterIsInstance
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import java.nio.file.Path

@Execution(CONCURRENT)
class VirtCustomizeCommandLineTest {

    @Test
    fun `should be instantiatable`(osImage: OperatingSystemImage) {
        expectCatching { createVirtCustomizeCommandLine(osImage) }.isSuccess()
    }

    @Test
    fun `should build proper command line`(osImage: OperatingSystemImage) {
        val commandLine = createVirtCustomizeCommandLine(osImage)
        expect {
            that(commandLine.workingDirectory).isEqualTo(osImage.file.parent)
            that(commandLine.customizationOptions).filterIsInstance<FirstBootOption>().hasSize(1).all {
                get { osImage.resolveOnHost(path) }.hasContent("""
                    sed -i 's/^\#Port 22${'$'}/Port 3421/g' /etc/ssh/sshd_config
                    systemctl enable getty@ttyGS0.service
                    
                """.trimIndent())
            }
            that(commandLine.toString()) {
                matchesCurlyPattern("""
                    virt-customize \
                    --add \
                    ${osImage.file.asString()} \
                    --verbose \
                    -x \
                    --append-line \
                    "/etc/sudoers.d/privacy:Defaults        lecture = never" \
                    --chmod \
                    0664:/chmod-file \
                    --chmod \
                    0664:/other/file \
                    --commands-from-file \
                    /commands/from/files-1 \
                    --commands-from-file \
                    /commands/from/files-2 \
                    --copy \
                    /source:/copy \
                    --copy-in \
                    /shared/from/host:/to/guest \
                    --copy-in \
                    /shared/some/file:/some \
                    --delete \
                    /delete/file1 \
                    --delete \
                    /delete/dir/2 \
                    --edit \
                    /etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/ \
                    --hostname \
                    the-machine \
                    --firstboot \
                    /shared/script-{}.sh \
                    --firstboot-command \
                    "command arg1 arg2" \
                    --firstboot-command \
                    boot-command \
                    --firstboot-install \
                    package1,package2 \
                    --firstboot-install \
                    package3 \
                    --hostname \
                    new-hostname \
                    --mkdir \
                    /new/dir \
                    --move \
                    /new/dir:/moved/dir \
                    --password \
                    "super-admin:password:super secure" \
                    --root-password \
                    disabled \
                    --ssh-inject \
                    file-user:file:file/key \
                    --ssh-inject \
                    string-user:string:string-key \
                    --timezone \
                    Europe/Berlin \
                    --touch \
                    /touch/file \
                    --write \
                    "/write/file:write content"
                """.trimIndent())
            }
        }
    }
}

private fun f(path: String): Path = Path.of(path)

internal fun createVirtCustomizeCommandLine(osImage: OperatingSystemImage) = VirtCustomizeCommandLine.build(osImage) {
    options {
        disk { it.file }
        quiet { off }
        verbose { on }
        trace { on }
    }
    customizationOptions {
        appendLine { osImage -> SharedPath.Disk.resolveRoot(osImage).resolve("/etc/sudoers.d/privacy") to "Defaults        lecture = never" }

        chmods { osImage -> "0664" to osImage.resolveOnDisk("/chmod-file") }
        chmods { osImage -> "0664" to osImage.resolveOnDisk("/other/file") }
        commandsFromFiles { osImage -> osImage.resolveOnDisk("/commands/from/files-1") }
        commandsFromFiles { osImage -> osImage.resolveOnDisk("/commands/from/files-2") }
        copy { it.resolveOnDisk("source") to it.resolveOnDisk("copy") }
        copyIn { it.resolveOnDocker("from/host") to it.resolveOnDisk("to/guest") }
        copyIn("/some/file")
        delete { it.resolveOnDisk("delete/file1") }
        delete { it.resolveOnDisk("/delete/dir/2") }
        edit { it.resolveOnDisk("/etc/dnf/dnf.conf") to "s/gpgcheck=1/gpgcheck=0/" }
        hostname { "the-machine" }
        firstBoot {
            !"""sed -i 's/^\#Port 22${'$'}/Port 3421/g' /etc/ssh/sshd_config"""
            !"""systemctl enable getty@ttyGS0.service"""
        }
        firstBootCommand { "command arg1 arg2" }
        firstBootCommand { "boot-command" }
        firstBootInstall {
            +"package1"
            +"package2"
        }
        firstBootInstall {
            +"package3"
        }
        hostname { "new-hostname" }
        mkdir { it.resolveOnDisk("new/dir") }
        move { it.resolveOnDisk("new/dir") to it.resolveOnDisk("moved/dir") }
        password { VirtCustomizeCustomizationOption.PasswordOption.byString("super-admin", "super secure") }
        rootPassword { VirtCustomizeCustomizationOption.RootPasswordOption.disabled() }
        sshInjectFile { osImage -> "file-user" to f("file/key") }
        sshInject("string-user", "string-key")
        timeZoneId("Europe/Berlin")
        touch { it.resolveOnDisk("touch/file") }
        write { it.resolveOnDisk("write/file") to "write content" }
    }
}
