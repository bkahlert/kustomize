package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.libguestfs.virtcustomize.file
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.content
import com.imgcstmzr.withTempDir
import koodies.terminal.AnsiCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.first
import strikt.assertions.hasSize


@Execution(ExecutionMode.CONCURRENT)
class FirstBootPatchTest {

    private val testBanner = "${AnsiCode.ESC}[40;90m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[46;96m░${AnsiCode.ESC}[49;39m" +
        "${AnsiCode.ESC}[44;94m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[42;92m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[43;93m░" +
        "${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[45;95m░${AnsiCode.ESC}[49;39m${AnsiCode.ESC}[41;91m░${AnsiCode.ESC}[49;39m " +
        "${AnsiCode.ESC}[96mTEST${AnsiCode.ESC}[39m"

    private val patch = FirstBootPatch("Test") {
        !"""echo "Type X to...""""
        !"""startx"""
    }

    @Test
    fun `should provide firstboot command`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch).customizations(osImage) {
            hasSize(1)
            filterIsInstance<VirtCustomizeCustomizationOption.FirstBootOption>().first().file.content {
                contains("echo \"$testBanner\"")
                contains("echo \"Type X to...\"")
                contains("startx")
            }
        }
    }
}

