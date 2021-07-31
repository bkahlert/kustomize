package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.containsFirstBootScriptFix
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.os.boot
import com.bkahlert.kustomize.test.E2E
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.shell.ShellScript
import koodies.test.Smoke
import koodies.test.withTempDir
import koodies.text.Banner.banner
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
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

    @E2E @Smoke @Test
    fun `should run firstboot scripts in correct order`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {

            osImage.patch(
                FirstBootPatch(
                    ShellScript("writing 'a'") { "echo 'a' > ${LinuxRoot.home / "file"}" },
                    ShellScript("appending 'b'") { "echo 'b' >> ${LinuxRoot.home / "file"}" },
                ),
                FirstBootPatch(
                    ShellScript("appending 'c'") { "echo 'c' >> ${LinuxRoot.home / "file"}" },
                    ShellScript("appending 'd'") { "echo 'd' >> /home/file" },
                ),
                FirstBootPatch(
                    ShellScript { shutdown },
                ),
            )
            osImage.boot()

            expectThat(osImage).mounted {
                path(LinuxRoot.home / "file") {
                    textContent.isEqualTo("""
                        a
                        b
                        c
                        d
                        
                    """.trimIndent())
                }
            }
        }
}
