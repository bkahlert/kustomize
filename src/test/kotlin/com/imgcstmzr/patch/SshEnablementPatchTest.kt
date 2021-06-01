package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.TouchCommand
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.isEqualTo

class SshEnablementPatchTest {

    @Test
    fun `should create ssh file`(osImage: OperatingSystemImage) {
        expectThat(SshEnablementPatch()).guestfishCommands(osImage) {
            first().isEqualTo(TouchCommand(DiskPath("/boot/ssh")))
        }
    }
}
