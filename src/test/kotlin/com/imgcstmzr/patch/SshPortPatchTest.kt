package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootCommandOption
import com.imgcstmzr.libguestfs.virtcustomize.containsFirstBootScriptFix
import com.imgcstmzr.runtime.OperatingSystemImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.last

@Execution(CONCURRENT)
class SshPortPatchTest {

    @Test
    fun `should create ssh file`(osImage: OperatingSystemImage) {
        expectThat(SshPortPatch(1234)).customizations(osImage) {
            containsFirstBootScriptFix()
            last().isEqualTo(FirstBootCommandOption("sed -i 's/^\\#Port 22\$/Port 1234/g' /etc/ssh/sshd_config"))
        }
    }
}
