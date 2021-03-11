package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.exists
import koodies.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class UsernamePatchTest {

    @FiveMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should log in with updated username`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val newUsername = "ella".also { check(it != osImage.defaultCredentials.username) { "$it is already the default username." } }

        patch(osImage, UsernamePatch(osImage.defaultCredentials.username, newUsername))

        expectThat(osImage.credentials).isEqualTo(OperatingSystem.Credentials(newUsername, osImage.defaultCredentials.password))
        expectThat(osImage).mounted(this) {
            path("/home/$newUsername") { exists() }
            path("/home/${osImage.defaultCredentials.username}") { not { exists() } }
        }
    }
}
