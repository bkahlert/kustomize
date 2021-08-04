package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.containsFirstBootScriptFix
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.test.E2E
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.shell.ShellScript
import koodies.test.Smoke
import koodies.test.withTempDir
import koodies.text.Banner.banner
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.last

class ShellScriptPatchTest {

    private val testFile = LinuxRoot.root / "shell-script-test.txt"

    private val shellScriptPatch = ShellScriptPatch(ShellScript("Test") {
        """
        touch $testFile
        echo 'Frank was here; went to get beer.' > $testFile
        """
    })

    @Test
    fun `should copy firstboot script`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(shellScriptPatch(osImage)).virtCustomizations {
            filterIsInstance<VirtCustomization.FirstBootOption>().any {
                file.textContent {
                    contains("echo '${banner("Test")}'")
                    contains("touch $testFile")
                    contains("echo 'Frank was here; went to get beer.' > $testFile")
                }
            }
            last().isA<VirtCustomization.FirstBootOption>().file.textContent {
                contains("echo '${banner("shutdown")}'")
                contains("'shutdown' '-h' 'now'")
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(shellScriptPatch(osImage)).virtCustomizations { containsFirstBootScriptFix() }
    }

    @E2E @Smoke @Test
    fun `should run shell script`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {

        osImage.patch(shellScriptPatch, BootAndShutdownPatch())

        expectThat(osImage).mounted {
            path(testFile) {
                textContent.isEqualTo("Frank was here; went to get beer.$LF")
            }
        }
    }

    @E2E @Smoke @Test
    fun `should run shell script22`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {

        osImage.patch(ShellScriptPatch(
            ShellScript("script 1", "echo 1"),
            ShellScript("script 2", "echo 2"),
            ShellScript("script 3", "echo 3"),
            ShellScript("script 4", "echo 4"),
            ShellScript("script 5", "echo 5"),
            ShellScript("script 6", "echo 6"),
            ShellScript("script 7", "echo 7"),
            ShellScript("script 8", "echo 8"),
            ShellScript("script 9", "echo 9"),
        ), BootAndShutdownPatch())

        expectThat(osImage).mounted {
            path(testFile) {
                textContent.isEqualTo("Frank was here; went to get beer.$LF")
            }
        }
    }
}
