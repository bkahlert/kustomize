package com.imgcstmzr.runtime

import com.imgcstmzr.test.DockerRequiring
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FifteenMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.containsAny
import com.imgcstmzr.test.rootCause
import koodies.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectCatching
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message
import java.util.concurrent.CompletionException

@Execution(CONCURRENT)
class OperatingSystemProcessTest {

    @FifteenMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should boot and run program in user session`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage, uniqueId: UniqueId) {

        val exitValue = osImage.execute(
            name = uniqueId.simple,
            logger = this,
            autoLogin = true,
            autoShutdown = true,
            osImage.compileScript("ping", "ping -c 1 \"imgcstmzr.com\"", "sleep 1", "echo 'test'")
        )

        expect {
            that(exitValue).isEqualTo(0)
            that(logged) {
                containsAny("PING imgcstmzr.com", ignoreCase = true)
                containsAny("shutting down", "reboot", ignoreCase = true)
            }
        }
    }

    @DockerRequiring @Test
    fun InMemoryLogger.`should terminate if obviously stuck`(@OS(OperatingSystems.TinyCore) osImage: OperatingSystemImage) {
        val corruptingString = "Booting Linux"
        val corruptedOsImage = OperatingSystemImage(object : OperatingSystem by osImage.operatingSystem {
            override val deadEndPattern: Regex get() = ".*$corruptingString.*".toRegex()
        }, osImage.credentials, osImage.file)

        expectCatching {
            corruptedOsImage.execute(logger = this)
        }
            .isFailure()
            .isA<CompletionException>()
            .rootCause
            .isA<IllegalStateException>()
            .message
            .isNotNull()
            .get { logged }
            .contains("Booting QEMU machine")
            .contains("The VM is stuck.")
    }
}
