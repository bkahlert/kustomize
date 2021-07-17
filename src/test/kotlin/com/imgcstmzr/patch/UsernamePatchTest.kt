package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.OperatingSystem
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.test.FifteenMinutesTimeout
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.java.exists

class UsernamePatchTest {

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @E2E @Test
    fun `should log in with updated username`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val newUsername = "ella".also { check(it != osImage.defaultCredentials.username) { "$it is already the default username." } }

        osImage.patch(UsernamePatch(osImage.defaultCredentials.username, newUsername))

        expectThat(osImage.credentials).isEqualTo(OperatingSystem.Credentials(newUsername, osImage.defaultCredentials.password))
        expectThat(osImage).mounted {
            path("/home/$newUsername") { exists() }
            path("/home/${osImage.defaultCredentials.username}") { not { exists() } }
        }
    }
}
