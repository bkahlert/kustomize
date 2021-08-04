package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.containsFirstBootScriptFix
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.test.withTempDir
import koodies.text.Banner.banner
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.last

class FirstBootPatchTest {

    @Test
    fun `should copy firstboot script`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        val patch = FirstBootPatch("Test") {
            echo("👏 🤓 👋")
            !"startx"
        }
        expectThat(patch(osImage)).virtCustomizations {
            last().isA<VirtCustomization.FirstBootOption>().file.textContent {
                contains("echo '${banner("Test")}'")
                contains("'echo' '👏 🤓 👋'")
                contains("startx")
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        val patch = FirstBootPatch("Test") {
            echo("👏 🤓 👋")
            !"startx"
        }
        expectThat(patch(osImage)).virtCustomizations { containsFirstBootScriptFix() }
    }

    // see ShellScriptPatchTest for further tests
}
