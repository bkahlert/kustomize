package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.io.path.hasContent
import koodies.test.CapturedOutput
import koodies.test.FiveMinutesTimeout
import koodies.test.SystemIOExclusive
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.java.exists

@SystemIOExclusive
class SshAuthorizationPatchTest {

    @FiveMinutesTimeout @DockerRequiring([DockerPiImage::class]) @E2E @Test
    fun `should add ssh key`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, output: CapturedOutput) {

        SshAuthorizationPatch("pi", listOf("123")).patch(osImage)

        output.all.contains("SSH key inject: pi")
        expectThat(osImage).mounted {
            path("/home/pi/.ssh/authorized_keys") {
                exists()
                hasContent("123$LF")
            }
        }
    }
}
