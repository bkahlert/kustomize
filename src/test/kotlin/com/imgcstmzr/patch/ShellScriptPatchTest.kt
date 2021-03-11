package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.libguestfs.virtcustomize.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.virtcustomize.file
import com.imgcstmzr.runtime.OperatingSystem
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
import koodies.terminal.AnsiCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.last

@Execution(CONCURRENT)
class ShellScriptPatchTest {

    private val testBanner = "${AnsiCode.ESC}[40;90m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[46;96m░${AnsiCode.ESC}[49;39m" +
        "${AnsiCode.ESC}[44;94m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[42;92m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[43;93m░" +
        "${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[45;95m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[41;91m░${AnsiCode.ESC}[49;39m " +
        "${AnsiCode.ESC}[96mTEST${AnsiCode.ESC}[39m"

    private val patch = ShellScriptPatch("Test") {
        !"touch /root/shell-script-test.txt"
        !"echo 'Frank was here; went to get beer.' > /root/shell-script-test.txt"
    }

    @Test
    fun `should copy firstboot script`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch).customizations(osImage) {
            last().isA<VirtCustomizeCustomizationOption.FirstBootOption>().file.content {
                contains("echo \"$testBanner\"")
                contains("touch /root/shell-script-test.txt")
                contains("echo 'Frank was here; went to get beer.' > /root/shell-script-test.txt")
                contains(OperatingSystem.DEFAULT_SHUTDOWN_COMMAND)
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch).customizations(osImage) { containsFirstBootScriptFix() }
    }

    // TODO    @DockerRequiring(["bkahlert/libguestfs"])
    @FiveMinutesTimeout @E2E @Smoke @Test
    fun `should run shell script`(logger: InMemoryLogger, uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {

            logger.patch(osImage, patch)

            expect {
                that(osImage).mounted(logger) {
                    path("/root/shell-script-test.txt") {
                        content.isEqualTo("Frank was here; went to get beer.")
                    }
                }
            }
        }
}
