package com.imgcstmzr.runtime

import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.string.containsAny
import com.bkahlert.koodies.test.junit.FifteenMinutesTimeout
import com.bkahlert.koodies.test.junit.JUnit
import com.bkahlert.koodies.test.junit.uniqueId
import com.imgcstmzr.E2E
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.OperatingSystems.TinyCore
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
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
    fun InMemoryLogger.`should boot and run program in user session`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {

        val exitValue = osImage.execute(
            name = JUnit.uniqueId,
            logger = this,
            autoLogin = true,
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
    fun InMemoryLogger.`should terminate if obviously stuck`(@OS(TinyCore) osImage: OperatingSystemImage) {
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
