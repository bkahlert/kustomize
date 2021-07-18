package com.imgcstmzr.os

import com.imgcstmzr.expectRendered
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.test.E2E
import koodies.docker.DockerRequiring
import koodies.exception.rootCause
import koodies.exec.Process.State.Excepted
import koodies.exec.Process.State.Exited.Succeeded
import koodies.junit.UniqueId
import koodies.test.Smoke
import koodies.test.expecting
import koodies.text.ansiRemoved
import org.junit.jupiter.api.Test
import strikt.assertions.contains
import strikt.assertions.containsIgnoringCase
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.message

class OperatingSystemProcessTest {

    @E2E @Smoke @Test
    fun `should boot and run program in user session`(uniqueId: UniqueId, @OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {

        expecting { osImage.boot(uniqueId.value, osImage.compileScript("print HOME", "printenv HOME")) } that {
            isA<Succeeded>()
        }

        expectRendered().ansiRemoved {
            containsIgnoringCase("/home/pi")
            containsIgnoringCase("Reached target Power-Off")
            containsIgnoringCase("System halted")
        }
    }

    @DockerRequiring([DockerPiImage::class]) @Test
    fun `should terminate if obviously stuck`(@OS(OperatingSystems.TinyCore) osImage: OperatingSystemImage) {
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

        expectRendered().ansiRemoved {
            contains("Booting QEMU machine")
            contains("The VM is stuck.")
        }
    }
}
