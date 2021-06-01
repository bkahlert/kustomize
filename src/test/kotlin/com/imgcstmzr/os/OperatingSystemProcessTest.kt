package com.imgcstmzr.os

import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.exception.rootCause
import koodies.exec.Process.State.Excepted
import koodies.exec.Process.State.Exited.Succeeded
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.FifteenMinutesTimeout
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.test.expecting
import org.junit.jupiter.api.Test
import strikt.assertions.contains
import strikt.assertions.containsIgnoringCase
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.message

class OperatingSystemProcessTest {

    @FifteenMinutesTimeout @E2E @Smoke @Test
    fun InMemoryLogger.`should boot and run program in user session`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage, uniqueId: UniqueId) {

        expecting { osImage.boot(uniqueId.value, this, osImage.compileScript("print HOME", "printenv HOME")) } that {
            isA<Succeeded>()
        }

        expectThatLogged {
            containsIgnoringCase("/home/pi")
            containsIgnoringCase("shutting down")
            containsIgnoringCase("reboot")
        }
    }

    @DockerRequiring([DockerPiImage::class]) @Test
    fun InMemoryLogger.`should terminate if obviously stuck`(@OS(OperatingSystems.TinyCore) osImage: OperatingSystemImage) {
        val corruptingString = "Booting Linux"
        val corruptedOsImage = OperatingSystemImage(object : OperatingSystem by osImage.operatingSystem {
            override val deadEndPattern: Regex get() = ".*$corruptingString.*".toRegex()
        }, osImage.credentials, osImage.file)

        expecting {
            corruptedOsImage.boot(name = null, logger = this)
        } that {
            isA<Excepted>()
                .get { exception }
                .isNotNull()
                .rootCause
                .isA<IllegalStateException>()
                .message
                .isNotNull()
        }

        expectThatLogged()
            .contains("Booting QEMU machine")
            .contains("The VM is stuck.")
    }
}
