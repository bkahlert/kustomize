package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.toPath
import com.imgcstmzr.libguestfs.guestfish.TouchCommand
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class SshEnablementPatchTest {

    @Test
    fun `should create ssh file`(osImage: OperatingSystemImage) {
        expectThat(SshEnablementPatch(RaspberryPiLite)).matches(guestfishCommandsAssertion = {
            first().get { invoke(osImage) }.isEqualTo(TouchCommand("/boot/ssh".toPath()))
        })
    }
}
