package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.exists
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.logging.logged
import koodies.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect

@Execution(CONCURRENT)
class SshAuthorizationPatchTest {

    @FiveMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should add ssh key`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {

        patch(osImage, SshAuthorizationPatch("pi", listOf("123")))

        expect {
            logged("SSH key inject: pi")
            that(osImage).mounted(this@`should add ssh key`) {
                path("/home/pi/.ssh/authorized_keys") {
                    exists()
                    hasContent("123\n")
                }
            }
        }
    }
}
