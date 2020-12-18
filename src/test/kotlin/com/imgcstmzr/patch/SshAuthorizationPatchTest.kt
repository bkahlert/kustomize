package com.imgcstmzr.patch

import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.imgcstmzr.E2E
import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.logged
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.exists

@Execution(CONCURRENT)
class SshAuthorizationPatchTest {

    @FiveMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should add ssh key`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val sshkey = "123"

        patch(osImage, SshAuthorizationPatch("pi", listOf(sshkey)))

        expect {
            logged(
                "SSH key inject: pi",
                "chown 1000 1000 \"/home/pi/.ssh/authorized_keys\"",
                "write_append \"/home/pi/.ssh/authorized_keys\" \"123\\x0a\"",
                "Finishing off",
            )
            that(osImage).mounted(this@`should add ssh key`) {
                path("/home/pi/.ssh/authorized_keys") {
                    exists()
                    hasContent("123\n")
                }
            }
        }
    }
}
