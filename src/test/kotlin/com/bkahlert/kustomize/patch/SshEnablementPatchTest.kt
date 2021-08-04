package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand.TouchCommand
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
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
