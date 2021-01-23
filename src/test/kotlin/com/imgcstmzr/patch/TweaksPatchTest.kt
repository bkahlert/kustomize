package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.AppendLineOption
import com.imgcstmzr.patch.TweaksPatch.Companion.APT_CONF_RETRIES
import com.imgcstmzr.runtime.OperatingSystemImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly

@Execution(CONCURRENT)
class TweaksPatchTest {

    @Test
    fun `should provide tweaks conf copying command`(osImage: OperatingSystemImage) {
        val patch = TweaksPatch(9)

        expect {
            that(patch).customizations(osImage) {
                containsExactly(listOf(AppendLineOption(APT_CONF_RETRIES, """APT::Acquire::Retries "9";""")))
            }
        }
    }
}
