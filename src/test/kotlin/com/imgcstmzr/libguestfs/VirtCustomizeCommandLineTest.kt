package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.ChmodOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.CopyInOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootCommandOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootInstallOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.MkdirOption
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystem
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.pathString
import koodies.content
import koodies.docker.DockerExec
import koodies.docker.asContainerPath
import koodies.exec.CommandLine
import koodies.exec.Executable
import koodies.io.path.asPath
import koodies.io.path.hasContent
import koodies.io.text
import koodies.shell.ShellScript
import koodies.test.toStringIsEqualTo
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.trim
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class VirtCustomizeCommandLineTest {

    @Test
    fun `should be instantiatable`(osImage: OperatingSystemImage) {
        expectCatching { createVirtCustomizeCommandLine(osImage) }.isSuccess()
    }

    @Test
    fun `should use absolute shared dir as guest working dir`(osImage: OperatingSystemImage) {
        val cmdLine = createVirtCustomizeCommandLine(osImage)
        expectThat(cmdLine.dockerOptions.workingDirectory).isEqualTo("/shared".asContainerPath())
    }

    @Test
    fun `should have correctly mapped arguments`(osImage: OperatingSystemImage) {
        val cmdLine = createVirtCustomizeCommandLine(osImage)
        expectThat(cmdLine).toStringIsEqualTo("""
            'docker' \
            'run' \
            '--entrypoint' \
            'virt-customize' \
            '--name' \
            'virt-customize--${cmdLine.dockerOptions.name!!.name.takeLast(4)}' \
            '--workdir' \
            '/shared' \
            '--rm' \
            '--interactive' \
            '--mount' \
            'type=bind,source=${osImage.exchangeDirectory},target=/shared' \
            '--mount' \
            'type=bind,source=${osImage.file},target=/images/disk.img' \
            'bkahlert/libguestfs@sha256:e8fdf16c69a9155b0e30cdc9b2f872232507f5461be2e7dff307f4c1b50faa20' \
            '--add' \
            '/images/disk.img' \
            '--verbose' \
            '-x' \
            '--append-line' \
            '/etc/sudoers.d/privacy:Defaults        lecture = never' \
            '--chmod' \
            '0664:/chmod-file' \
            '--chmod' \
            '0664:/other/file' \
            '--commands-from-file' \
            'commands/from/files-1' \
            '--commands-from-file' \
            'commands/from/files-2' \
            '--copy' \
            '/source:/copy' \
            '--mkdir' \
            '/from' \
            '--copy-in' \
            'from/host:/from' \
            '--mkdir' \
            '/some' \
            '--copy-in' \
            'some/file:/some' \
            '--delete' \
            '/delete/file1' \
            '--delete' \
            '/delete/dir/2' \
            '--edit' \
            '/etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/' \
            '--hostname' \
            'the-machine' \
            '--mkdir' \
            '/usr/lib/virt-sysprep/scripts' \
            '--copy-in' \
            'usr/lib/virt-sysprep/scripts/0000---fix-order---:/usr/lib/virt-sysprep/scripts' \
            '--chmod' \
            '0755:/usr/lib/virt-sysprep/scripts/0000---fix-order---' \
            '--mkdir' \
            '/etc/systemd/system' \
            '--copy-in' \
            'etc/systemd/system/firstboot-wait.service:/etc/systemd/system' \
            '--mkdir' \
            '/etc/systemd/system/multi-user.target.wants' \
            '--link' \
            '/etc/systemd/system/firstboot-wait.service:/etc/systemd/system/multi-user.target.wants/firstboot-wait.service' \
            '--mkdir' \
            '/etc/systemd/scripts' \
            '--copy-in' \
            'etc/systemd/scripts/firstboot-wait.sh:/etc/systemd/scripts' \
            '--chmod' \
            '0755:/etc/systemd/scripts/firstboot-wait.sh' \
            '--firstboot' \
            '${cmdLine.firstbootScript(emptyMap(), osImage.exchangeDirectory).fileName}' \
            '--firstboot-command' \
            'command arg1 arg2' \
            '--firstboot-command' \
            'boot-command' \
            '--firstboot-install' \
            'package1,package2' \
            '--firstboot-install' \
            'package3' \
            '--hostname' \
            'new-hostname' \
            '--link' \
            '/target:/link' \
            '--mkdir' \
            '/new/dir' \
            '--move' \
            '/new/dir:/moved/dir' \
            '--password' \
            'super-admin:password:super secure' \
            '--root-password' \
            'disabled' \
            '--ssh-inject' \
            'file-user:file:file/key' \
            '--ssh-inject' \
            'string-user:string:string-key' \
            '--timezone' \
            'Europe/Berlin' \
            '--touch' \
            '/touch/file' \
            '--write' \
            '/write/file:write content'
        """.trimIndent())
    }

    @Test
    fun `should store firstboot scripts in absolute guest shared dir`(osImage: OperatingSystemImage) {
        val scriptLocation = osImage.exchangeDirectory.resolve("usr/lib/virt-sysprep/scripts/0000---fix-order---")
        check(!scriptLocation.exists())

        createVirtCustomizeCommandLine(osImage)

        expectThat(scriptLocation).hasContent(FirstBootFix.text + LF)
    }
}

internal fun createVirtCustomizeCommandLine(osImage: OperatingSystemImage): VirtCustomizeCommandLine = VirtCustomizeCommandLine.build(osImage) {
    options {
        disk { it.file }
        quiet { off }
        verbose { on }
        trace { on }
    }
    customizations {
        appendLine { "Defaults        lecture = never" to LinuxRoot.etc / "sudoers.d" / "privacy" }

        chmods { "0664" to LinuxRoot / "chmod-file" }
        chmods { "0664" to LinuxRoot / "other" / "file" }
        commandsFromFiles { osImage -> osImage.hostPath(LinuxRoot / "commands" / "from/files-1") }
        commandsFromFiles { osImage -> osImage.hostPath(LinuxRoot / "commands" / "from/files-2") }
        copy { LinuxRoot / "source" to LinuxRoot / "copy" }
        copyIn { it.hostPath(LinuxRoot / "from" / "host") to LinuxRoot / "from" }
        copyIn(LinuxRoot / "some/file")
        delete { LinuxRoot / "delete" / "file1" }
        delete { LinuxRoot / "delete" / "dir" / "2" }
        edit { LinuxRoot.etc / "dnf" / "dnf.conf" to "s/gpgcheck=1/gpgcheck=0/" }
        hostname { "the-machine" }
        firstBoot {
            """
                sed -i 's/^\#Port 22${'$'}/Port 3421/g' /etc/ssh/sshd_config
                systemctl enable serial-getty@ttyGS0.service
            """
        }
        firstBootCommand { "command arg1 arg2" }
        firstBootCommand { "boot-command" }
        firstBootInstall { listOf("package1", "package2") }
        firstBootInstall { listOf("package3") }
        hostname { "new-hostname" }
        link { LinuxRoot / "link" to LinuxRoot / "target" }
        mkdir { LinuxRoot / "new" / "dir" }
        move { LinuxRoot / "new" / "dir" to LinuxRoot / "moved" / "dir" }
        password(Customization.PasswordOption.byString("super-admin", "super secure"))
        rootPassword(Customization.RootPasswordOption.disabled())
        sshInjectFile { "file-user" to Path.of("file/key") }
        sshInject { "string-user" to "string-key" }
        timeZoneId { "Europe/Berlin" }
        touch { LinuxRoot / "touch" / "file" }
        write { LinuxRoot / "write" / "file" to "write content" }
    }
}

private fun CommandLine.firstbootScript(): Path =
    commandLineParts.single { it.contains("script-") }.asPath()

private fun Executable<DockerExec>.firstbootScript(
    environment: Map<String, String>,
    workingDirectory: Path?,
): Path = toCommandLine(environment, workingDirectory).firstbootScript()

fun Assertion.Builder<List<Customization>>.containsFirstBootScriptFix() {
    filterIsInstance<MkdirOption>().any { dir.pathString.isEqualTo(FirstBootFix.FIRSTBOOT_SCRIPTS.pathString) }
    filterIsInstance<CopyInOption>().any { localPath.content.trim().isEqualTo(FirstBootFix.text.trim()) }
    filterIsInstance<ChmodOption>().any { setsPermission("0755", FirstBootFix.FIRSTBOOT_FIX) }
}

fun Assertion.Builder<List<Customization>>.containsFirstBootShutdownCommand() {
    filterIsInstance<FirstBootOption>().any {
        file.content.contains(OperatingSystem.DEFAULT_SHUTDOWN_COMMAND)
    }
}


val Assertion.Builder<MkdirOption>.dir: Assertion.Builder<DiskPath>
    get() = get("dir %s") { dir }

fun Assertion.Builder<ChmodOption>.setsPermission(expectedPermission: String, expectedFile: DiskPath) =
    get("dir %s") { permission to file }.isEqualTo(expectedPermission to expectedFile)

val Assertion.Builder<CopyInOption>.localPath: Assertion.Builder<Path /* = java.nio.file.Path */>
    get() = get("local path %s") { localPath }

val Assertion.Builder<FirstBootInstallOption>.packages: Assertion.Builder<List<String>>
    get() = get("packages %s") { packages }

val Assertion.Builder<FirstBootCommandOption>.command: Assertion.Builder<String>
    get() = get("command %s") { command }

val Assertion.Builder<FirstBootOption>.file: Assertion.Builder<Path /* = java.nio.file.Path */>
    get() = get("file %s") { path }

val Assertion.Builder<FirstBootOption>.script: Assertion.Builder<ShellScript>
    get() = get("script %s") { ShellScript(content = path.readText()) }
