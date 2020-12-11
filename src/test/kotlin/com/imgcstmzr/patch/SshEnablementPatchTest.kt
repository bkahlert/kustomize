package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.copyOut
import com.imgcstmzr.libguestfs.guestfish.TouchCommand
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.first
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class SshEnablementPatchTest {

    @Test
    fun `should create ssh file`() {
        expectThat(SshEnablementPatch(RaspberryPiLite)).matches(guestfishCommandsAssertion = {
            first().isEqualTo(TouchCommand("/boot/ssh".toPath()))
        })
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should create ssh file on disk`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        with(SshEnablementPatch(osImage.operatingSystem)) {
            patch(osImage)
        }
        osImage.copyOut("/boot/ssh")
        expectThat(osImage.resolveOnHost("/boot/ssh")).exists()
    }
}
