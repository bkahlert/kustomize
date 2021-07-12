package com.imgcstmzr.os

import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.exception.rootCause
import koodies.exec.Process.State.Excepted
import koodies.exec.Process.State.Exited.Succeeded
import koodies.junit.UniqueId
import koodies.test.CapturedOutput
import koodies.test.FifteenMinutesTimeout
import koodies.test.Smoke
import koodies.test.SystemIOExclusive
import koodies.test.expecting
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsIgnoringCase
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.message

@SystemIOExclusive
class OperatingSystemProcessTest {

    @FifteenMinutesTimeout @E2E @Smoke @Test
    fun `should boot and run program in user session`(
        @OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage,
        uniqueId: UniqueId,
        output: CapturedOutput,
    ) {

        expecting { osImage.boot(uniqueId.value, osImage.compileScript("print HOME", "printenv HOME")) } that {
            isA<Succeeded>()
        }

        expectThat(output.all) {
            containsIgnoringCase("/home/pi")
            containsIgnoringCase("Reached target Power-Off")
            containsIgnoringCase("System halted")
        }
    }

    @DockerRequiring([DockerPiImage::class]) @Test
    fun `should terminate if obviously stuck`(
        @OS(OperatingSystems.TinyCore) osImage: OperatingSystemImage,
        output: CapturedOutput,
    ) {
        val corruptingString = "Booting Linux"
        val corruptedOsImage = OperatingSystemImage(object : OperatingSystem by osImage.operatingSystem {
            override val deadEndPattern: Regex get() = ".*$corruptingString.*".toRegex()
        }, osImage.credentials, osImage.file)

        expecting {
            corruptedOsImage.boot(name = null)
        } that {
            isA<Excepted>()
                .get { exception }
                .isNotNull()
                .rootCause
                .isA<IllegalStateException>()
                .message
                .isNotNull()
        }

        expectThat(output.all) {
            contains("Booting QEMU machine")
            contains("The VM is stuck.")
        }
    }
}
