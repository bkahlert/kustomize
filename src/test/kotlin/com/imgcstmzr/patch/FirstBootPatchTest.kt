package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.file
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.content
import koodies.docker.DockerRequiring
import koodies.junit.UniqueId
import koodies.shell.ShellScript
import koodies.test.FifteenMinutesTimeout
import koodies.test.Smoke
import koodies.test.withTempDir
import koodies.text.Banner.banner
import koodies.text.singleQuoted
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.last

class FirstBootPatchTest {

    private val patch = FirstBootPatch("Test") {
        echo("Type X to …")
        !"startx"
    }

    @Test
    fun `should copy firstboot script`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch(osImage)).customizations {
            last().isA<Customization.FirstBootOption>().file.content {
                contains("echo ${banner("Test").singleQuoted}")
                contains("'echo' 'Type X to …'")
                contains("startx")
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch(osImage)).customizations { containsFirstBootScriptFix() }
    }

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class, DockerPiImage::class]) @E2E @Smoke @Test
    fun `should run firstboot scripts in correct order`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {

            osImage.patch(
                FirstBootPatch(
                    ShellScript("writing 'a'") { "echo 'a' > /home/file" },
                    ShellScript("appending 'b'") { "echo 'b' >> /home/file" },
                ),
                FirstBootPatch(
                    ShellScript("appending 'c'") { "echo 'c' >> /home/file" },
                    ShellScript("appending 'd'") { "echo 'd' >> /home/file" },
                ),
            )

            expect {
                that(osImage).booted {
                    command("echo /home/file");
                    { true }
                }
                that(osImage).mounted {
                    path("/home/file") {
                        content.isEqualTo("""
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
