package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.libguestfs.virtcustomize.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.virtcustomize.file
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.Smoke
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.content
import com.imgcstmzr.withTempDir
import koodies.logging.InMemoryLogger
import koodies.shell.ShellScript
import koodies.terminal.AnsiCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.last


@Execution(ExecutionMode.CONCURRENT)
class FirstBootPatchTest {

    private val testBanner = "${AnsiCode.ESC}[40;90m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[46;96m░${AnsiCode.ESC}[49;39m" +
        "${AnsiCode.ESC}[44;94m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[42;92m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[43;93m░" +
        "${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[45;95m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[41;91m░${AnsiCode.ESC}[49;39m " +
        "${AnsiCode.ESC}[96mTEST${AnsiCode.ESC}[39m"

    private val patch = FirstBootPatch("Test") {
        !"""echo "Type X to …""""
        !"""startx"""
    }

    @Test
    fun `should copy firstboot script`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch).customizations(osImage) {
            last().isA<VirtCustomizeCustomizationOption.FirstBootOption>().file.content {
                contains("echo \"$testBanner\"")
                contains("echo \"Type X to …\"")
                contains("startx")
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch).customizations(osImage) { containsFirstBootScriptFix() }
    }

    // TODO    @DockerRequiring(["bkahlert/libguestfs"])
    @FiveMinutesTimeout @E2E @Smoke @Test
    fun `should run firstboot scripts in correct order`(logger: InMemoryLogger, uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {

            FirstBootPatch(
                ShellScript("writing 'a'") { !"echo 'a' > /home/file" },
                ShellScript("appending 'b'") { !"echo 'b' >> /home/file" },
            ).patch(osImage, logger)

            FirstBootPatch(
                ShellScript("appending 'c'") { !"echo 'c' >> /home/file" },
                ShellScript("appending 'd'") { !"echo 'd' >> /home/file" },
            ).patch(osImage, logger)


            expect {
                that(osImage).booted(logger) {
                    command("echo /home/file");
                    { true }
                }
                that(osImage).mounted(logger) {
                    path("/home/file") {
                        content.isEqualTo("abcd")
                    }
                }
            }
        }
}

