package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.exists
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.toStringIsEqualTo
import koodies.concurrent.process.CommandLine
import koodies.docker.asContainerPath
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.shell.ShellScript
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.filterIsInstance
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isSuccess
import strikt.assertions.single
import java.nio.file.Path
import kotlin.io.path.readText

@Execution(CONCURRENT)
class VirtCustomizeCommandLineTest {

    @Test
    fun `should be instantiatable`(osImage: OperatingSystemImage) {
        expectCatching { createVirtCustomizeCommandLine(osImage) }.isSuccess()
    }

    @Nested
    inner class AsCommandLine {

        @Test
        fun `should use sibling shared dir as working dir`(osImage: OperatingSystemImage) {
            val cmdLine = createVirtCustomizeCommandLine(osImage)
            expectThat(cmdLine.workingDirectory).isEqualTo(osImage.file.resolveSibling("shared"))
        }

        @Test
        fun `should store firstboot scripts in shared dir`(osImage: OperatingSystemImage) {
            val cmdLine = createVirtCustomizeCommandLine(osImage)
            expectThat(cmdLine.customizationOptions).filterIsInstance<VirtCustomizeCustomizationOption.FirstBootOption>().single()
                .file
                .hasContent("""
                    sed -i 's/^\#Port 22${'$'}/Port 3421/g' /etc/ssh/sshd_config
                    systemctl enable getty@ttyGS0.service
                    
                """.trimIndent())
                .get { parent }.isEqualTo(osImage.hostPath(DiskPath("/")))
        }

        @Test
        fun `should have correctly mapped arguments`(osImage: OperatingSystemImage) {
            val cmdLine = createVirtCustomizeCommandLine(osImage)
            expectThat(cmdLine).toStringIsEqualTo("""
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
                    commands/from/files-1 \
                    --commands-from-file \
                    commands/from/files-2 \
                    --copy \
                    /source:/copy \
                    --mkdir \
                    /from \
                    --copy-in \
                    from/host:/from \
                    --mkdir \
                    /some \
                    --copy-in \
                    some/file:/some \
                    --delete \
                    /delete/file1 \
                    --delete \
                    /delete/dir/2 \
                    --edit \
                    /etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/ \
                    --hostname \
                    the-machine \
                    --mkdir \
                    /usr/lib/virt-sysprep/scripts \
                    --copy-in \
                    usr/lib/virt-sysprep/scripts/0000---fix-order---:/usr/lib/virt-sysprep/scripts \
                    --chmod \
                    0755:/usr/lib/virt-sysprep/scripts/0000---fix-order--- \
                    --firstboot \
                    ${cmdLine.firstbootScript().fileName.asString()} \
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

    @Nested
    inner class AsDockerCommandLine {

        @Test
        fun `should use sibling shared dir as working dir`(osImage: OperatingSystemImage) {
            val cmdLine = createVirtCustomizeCommandLine(osImage).dockerCommandLine()
            expectThat(cmdLine.workingDirectory).isEqualTo(osImage.file.resolveSibling("shared"))
        }

        @Test
        fun `should use absolute shared dir as guest working dir`(osImage: OperatingSystemImage) {
            val cmdLine = createVirtCustomizeCommandLine(osImage).dockerCommandLine()
            expectThat(cmdLine.options.workingDirectory).isEqualTo("/shared".asContainerPath())
        }

        @Test
        fun `should store firstboot scripts in absolute guest shared dir`(osImage: OperatingSystemImage) {
            val cmdLine = createVirtCustomizeCommandLine(osImage).dockerCommandLine()
            expectThat(cmdLine.firstbootScript()).get { cmdLine.workingDirectory.resolve(this).parent }.isNotNull().exists()
        }

        @Test
        fun `should have correctly mapped arguments`(osImage: OperatingSystemImage) {
            val cmdLine = createVirtCustomizeCommandLine(osImage).dockerCommandLine()
            expectThat(cmdLine).toStringIsEqualTo("""
                    docker \
                    run \
                    --entrypoint \
                    virt-customize \
                    --name \
                    libguestfs-virt-customize-${cmdLine.options.name.toString().takeLast(4)} \
                    -w \
                    /shared \
                    --rm \
                    -i \
                    --mount \
                    type=bind,source=${Libguestfs.mountRootForDisk(osImage.file)},target=/shared \
                    --mount \
                    type=bind,source=${osImage.file},target=/images/disk.img \
                    bkahlert/libguestfs@sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a \
                    --add \
                    /images/disk.img \
                    --verbose \
                    -x \
                    --append-line \
                    "/etc/sudoers.d/privacy:Defaults        lecture = never" \
                    --chmod \
                    0664:/chmod-file \
                    --chmod \
                    0664:/other/file \
                    --commands-from-file \
                    commands/from/files-1 \
                    --commands-from-file \
                    commands/from/files-2 \
                    --copy \
                    /source:/copy \
                    --mkdir \
                    /from \
                    --copy-in \
                    from/host:/from \
                    --mkdir \
                    /some \
                    --copy-in \
                    some/file:/some \
                    --delete \
                    /delete/file1 \
                    --delete \
                    /delete/dir/2 \
                    --edit \
                    /etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/ \
                    --hostname \
                    the-machine \
                    --mkdir \
                    /usr/lib/virt-sysprep/scripts \
                    --copy-in \
                    usr/lib/virt-sysprep/scripts/0000---fix-order---:/usr/lib/virt-sysprep/scripts \
                    --chmod \
                    0755:/usr/lib/virt-sysprep/scripts/0000---fix-order--- \
                    --firstboot \
                    ${cmdLine.firstbootScript().fileName} \
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

internal fun createVirtCustomizeCommandLine(osImage: OperatingSystemImage) = VirtCustomizeCommandLine.build(osImage) {
    options {
        disk { it.file }
        quiet { off }
        verbose { on }
        trace { on }
    }
    customizationOptions {
        appendLine { osImage -> "Defaults        lecture = never" to DiskPath("/etc/sudoers.d/privacy") }

        chmods { osImage -> "0664" to DiskPath("/chmod-file") }
        chmods { osImage -> "0664" to DiskPath("/other/file") }
        commandsFromFiles { osImage -> osImage.hostPath(DiskPath("/commands/from/files-1")) }
        commandsFromFiles { osImage -> osImage.hostPath(DiskPath("/commands/from/files-2")) }
        copy { DiskPath("/source") to DiskPath("/copy") }
        copyIn { it.hostPath(DiskPath("/from/host")) to DiskPath("/from") }
        copyIn(DiskPath("/some/file"))
        delete { DiskPath("/delete/file1") }
        delete { DiskPath("/delete/dir/2") }
        edit { DiskPath("/etc/dnf/dnf.conf") to "s/gpgcheck=1/gpgcheck=0/" }
        hostname { "the-machine" }
        firstBoot {
            !"""sed -i 's/^\#Port 22${'$'}/Port 3421/g' /etc/ssh/sshd_config"""
            !"""systemctl enable getty@ttyGS0.service"""
        }
        firstBootCommand { "command arg1 arg2" }
        firstBootCommand { "boot-command" }
        firstBootInstall { listOf("package1", "package2") }
        firstBootInstall { listOf("package3") }
        hostname { "new-hostname" }
        mkdir { DiskPath("/new/dir") }
        move { DiskPath("/new/dir") to DiskPath("/moved/dir") }
        password(VirtCustomizeCustomizationOption.PasswordOption.byString("super-admin", "super secure"))
        rootPassword(VirtCustomizeCustomizationOption.RootPasswordOption.disabled())
        sshInjectFile { osImage -> "file-user" to Path.of("file/key") }
        sshInject { "string-user" to "string-key" }
        timeZoneId { "Europe/Berlin" }
        touch { DiskPath("/touch/file") }
        write { DiskPath("/write/file") to "write content" }
    }
}

private fun CommandLine.firstbootScript(): Path =
    commandLineParts.single { it.contains("script-") }.asPath()

val Assertion.Builder<VirtCustomizeCustomizationOption.MkdirOption>.dir
    get() = get("dir %s") { dir }

fun Assertion.Builder<VirtCustomizeCustomizationOption.ChmodOption>.setsPermission(expectedPermission: String, expectedFile: DiskPath) =
    get("dir %s") { permission to file }.isEqualTo(expectedPermission to expectedFile)

val Assertion.Builder<VirtCustomizeCustomizationOption.CopyInOption>.localPath
    get() = get("local path %s") { localPath }

val Assertion.Builder<VirtCustomizeCustomizationOption.FirstBootOption>.file
    get() = get("file %s") { path }

val Assertion.Builder<VirtCustomizeCustomizationOption.FirstBootOption>.script
    get() = get("script %s") { ShellScript(content = path.readText()) }

