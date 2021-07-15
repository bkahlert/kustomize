package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.file
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.OperatingSystem
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.content
import koodies.docker.DockerRequiring
import koodies.junit.UniqueId
import koodies.test.FiveMinutesTimeout
import koodies.test.Smoke
import koodies.test.withTempDir
import koodies.text.Banner.banner
import koodies.text.LineSeparators.LF
import koodies.text.singleQuoted
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.last

class ShellScriptPatchTest {

    private val shellScriptPatch = ShellScriptPatch("Test") {
        """
        touch /root/shell-script-test.txt
        echo 'Frank was here; went to get beer.' > /root/shell-script-test.txt
        """
    }

    @Test
    fun `should copy firstboot script`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(shellScriptPatch).customizations(osImage) {
            last().isA<Customization.FirstBootOption>().file.content {
                contains("echo ${banner("Test").singleQuoted}")
                contains("touch /root/shell-script-test.txt")
                contains("echo 'Frank was here; went to get beer.' > /root/shell-script-test.txt")
                contains(OperatingSystem.DEFAULT_SHUTDOWN_COMMAND.shellCommand)
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(shellScriptPatch).customizations(osImage) { containsFirstBootScriptFix() }
    }

    @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @E2E @Smoke @Test
    fun `should run shell script`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {

        shellScriptPatch.patch(osImage)

        expectThat(osImage).mounted {
            path("/root/shell-script-test.txt") {
                content.isEqualTo("Frank was here; went to get beer.$LF")
            }
        }
    }
}
