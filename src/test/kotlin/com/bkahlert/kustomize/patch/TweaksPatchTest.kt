package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand.WriteAppendCommand
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.containsExactly

class TweaksPatchTest {

    @Test
    fun `should provide tweaks conf copying command`(osImage: OperatingSystemImage) {
        val patch = TweaksPatch(9).invoke(osImage)

        expect {
            that(patch).guestfishCommands {
                containsExactly(listOf(WriteAppendCommand(LinuxRoot.etc.apt.apt_conf_d.`80_retries`, """APT::Acquire::Retries "9";$LF""")))
            }
        }
    }
}
