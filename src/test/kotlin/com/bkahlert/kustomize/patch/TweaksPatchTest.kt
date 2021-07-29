package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Customization.AppendLineOption
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.containsExactly

class TweaksPatchTest {

    @Test
    fun `should provide tweaks conf copying command`(osImage: OperatingSystemImage) {
        val patch = TweaksPatch(9).invoke(osImage)

        expect {
            that(patch).diskCustomizations {
                containsExactly(listOf(AppendLineOption(LinuxRoot.etc.apt.apt_conf_d.`80_retries`, """APT::Acquire::Retries "9";""")))
            }
        }
    }
}
