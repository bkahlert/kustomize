package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.containsFirstBootScriptFix
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystem
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.test.E2E
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.test.Smoke
import koodies.test.withTempDir
import koodies.text.Banner.banner
import koodies.text.LineSeparators.LF
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
        expectThat(shellScriptPatch(osImage)).diskCustomizations {
            last().isA<VirtCustomization.FirstBootOption>().file.textContent {
                contains("""echo '"'"'${banner("Test")}'"'"'""")
                contains("touch $testFile")
                contains("""echo '"'"'Frank was here; went to get beer.'"'"' > $testFile""")
                contains(OperatingSystem.DEFAULT_SHUTDOWN_COMMAND.shellCommand)
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(shellScriptPatch(osImage)).diskCustomizations { containsFirstBootScriptFix() }
    }

    @E2E @Smoke @Test
    fun `should run shell script`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {

        osImage.patch(shellScriptPatch)

        expectThat(osImage).mounted {
            path(testFile) {
                textContent.isEqualTo("Frank was here; went to get beer.$LF")
            }
        }
    }
}
