package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootOption
import com.imgcstmzr.libguestfs.virtcustomize.file
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FifteenMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.content
import com.imgcstmzr.test.logging.expectThatLogged
import com.imgcstmzr.withTempDir
import koodies.logging.InMemoryLogger
import koodies.terminal.AnsiCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.first
import strikt.assertions.hasSize

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
    fun `should provide firstboot command`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch).customizations(osImage) {
            hasSize(1)
            filterIsInstance<FirstBootOption>().first().file.content {
                contains("echo \"$testBanner\"")
                contains("touch /root/shell-script-test.txt")
                contains("echo 'Frank was here; went to get beer.' > /root/shell-script-test.txt")
                contains(OperatingSystem.DEFAULT_SHUTDOWN_COMMAND)
            }
        }
    }

    @FifteenMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should run shell script`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage, uniqueId: UniqueId) =
        withTempDir(uniqueId) {
            patch(osImage, patch)
            osImage.boot(this@`should run shell script`, osImage.compileScript("read log", "sudo cat ~root/virt-sysprep-firstboot.log"))
            expectThatLogged().contains("=== Running /usr/lib/virt-sysprep/scripts/0001--shared")
        }
}
