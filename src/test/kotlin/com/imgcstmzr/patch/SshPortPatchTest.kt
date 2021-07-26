package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootCommandOption
import com.imgcstmzr.libguestfs.containsFirstBootScriptFix
import com.imgcstmzr.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.last

class SshPortPatchTest {

    @Test
    fun `should create ssh file`(osImage: OperatingSystemImage) {
        val patch = SshPortPatch(1234).invoke(osImage)
        expectThat(patch).diskCustomizations {
            containsFirstBootScriptFix()
            last().isEqualTo(FirstBootCommandOption("sed -i 's/^\\#Port 22\$/Port 1234/g' /etc/ssh/sshd_config"))
        }
    }
}
