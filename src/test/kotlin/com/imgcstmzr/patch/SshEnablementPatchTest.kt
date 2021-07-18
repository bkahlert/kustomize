package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.TouchCommand
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.isEqualTo

class SshEnablementPatchTest {

    @Test
    fun `should create ssh file`(osImage: OperatingSystemImage) {
        val patch = SshEnablementPatch().invoke(osImage)
        expectThat(patch).guestfishCommands {
            first().isEqualTo(TouchCommand(LinuxRoot.boot.ssh))
        }
    }
}
