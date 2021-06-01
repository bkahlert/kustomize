package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.io.path.hasContent
import koodies.logging.InMemoryLogger
import koodies.logging.expectLogged
import koodies.test.FiveMinutesTimeout
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.java.exists

class SshAuthorizationPatchTest {

    @FiveMinutesTimeout @DockerRequiring([DockerPiImage::class]) @E2E @Test
    fun InMemoryLogger.`should add ssh key`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {

        SshAuthorizationPatch("pi", listOf("123")).patch(osImage)

        expectLogged.contains("SSH key inject: pi")
        expectThat(osImage).mounted {
            path("/home/pi/.ssh/authorized_keys") {
                exists()
                hasContent("123$LF")
            }
        }
    }
}
