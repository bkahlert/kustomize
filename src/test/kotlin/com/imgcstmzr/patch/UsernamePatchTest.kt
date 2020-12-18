package com.imgcstmzr.patch

import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.imgcstmzr.E2E
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.copyOut
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isEqualTo
import strikt.assertions.resolve

@Execution(CONCURRENT)
class UsernamePatchTest {

    @FiveMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should log in with updated username`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val newUsername = "ella".also { check(it != osImage.defaultCredentials.username) { "$it is already the default username." } }

        patch(osImage, UsernamePatch(osImage.defaultCredentials.username, newUsername))

        expectThat(osImage.credentials).isEqualTo(OperatingSystem.Credentials(newUsername, osImage.defaultCredentials.password))
        expectThat(with(osImage) { copyOut("/home") }) {
            resolve(newUsername).exists()
            resolve(osImage.defaultCredentials.username).not { exists() }
        }
    }
}
