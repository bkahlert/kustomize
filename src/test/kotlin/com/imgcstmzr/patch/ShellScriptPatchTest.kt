package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.file
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystem
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import koodies.content
import koodies.junit.UniqueId
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

    private val testFile = LinuxRoot.root / "shell-script-test.txt"

    private val shellScriptPatch = ShellScriptPatch("Test") {
        """
        touch $testFile
        echo 'Frank was here; went to get beer.' > $testFile
        """
    }

    @Test
    fun `should copy firstboot script`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(shellScriptPatch(osImage)).customizations {
            last().isA<Customization.FirstBootOption>().file.content {
                contains("echo ${banner("Test").singleQuoted}")
                contains("touch $testFile")
                contains("echo 'Frank was here; went to get beer.' > $testFile")
                contains(OperatingSystem.DEFAULT_SHUTDOWN_COMMAND.shellCommand)
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(shellScriptPatch(osImage)).customizations { containsFirstBootScriptFix() }
    }

    @E2E @Smoke @Test
    fun `should run shell script`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {

        osImage.patch(shellScriptPatch)

        expectThat(osImage).mounted {
            path(testFile) {
                content.isEqualTo("Frank was here; went to get beer.$LF")
            }
        }
    }
}
