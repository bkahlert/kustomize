package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.file
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.shell.ShellScript
import koodies.test.Smoke
import koodies.test.withTempDir
import koodies.text.Banner.banner
import org.junit.jupiter.api.Test
import strikt.api.expect
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
        expectThat(patch(osImage)).diskCustomizations {
            last().isA<Customization.FirstBootOption>().file.textContent {
                contains("""echo '"'"'${banner("Test")}'"'"'""")
                contains("""'"'"'echo'"'"' '"'"'👏 🤓 👋'"'"'""")
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
        expectThat(patch(osImage)).diskCustomizations { containsFirstBootScriptFix() }
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
            )

            expect {
                that(osImage).booted {
                    command("echo /home/file");
                    { true }
                }
                that(osImage).mounted {
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
}
