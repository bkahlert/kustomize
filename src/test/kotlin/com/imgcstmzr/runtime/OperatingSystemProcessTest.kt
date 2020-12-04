package com.imgcstmzr.runtime

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.docker.DockerContainerName
import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.string.containsAny
import com.bkahlert.koodies.test.junit.FifteenMinutesTimeout
import com.imgcstmzr.E2E
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.OperatingSystems.TinyCore
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.AfterAll
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
import java.util.concurrent.ExecutionException
import kotlin.reflect.KFunction

@Execution(CONCURRENT)
class OperatingSystemProcessTest {

    @FifteenMinutesTimeout @E2E @Test
    fun `should boot and run program in user session`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, logger: InMemoryLogger<*>) {

        val exitValue = osImage.execute(
            name = name(::`should boot and run program in user session`),
            logger = logger,
            autoLogin = true,
            osImage.compileScript("ping", "ping -c 1 \"imgcstmzr.com\"", "sleep 1", "echo 'test'")
        )

        expect {
            that(exitValue).isEqualTo(0)
            that(logger.logged) {
                containsAny("PING imgcstmzr.com", ignoreCase = true)
                containsAny("shutting down", "reboot", ignoreCase = true)
            }
        }
    }

    @DockerRequiring @Test
    fun `should terminate if obviously stuck`(@OS(TinyCore) osImage: OperatingSystemImage, logger: InMemoryLogger<*>) {
        val corruptingString = "Booting Linux"
        val corruptedOsImage = OperatingSystemImage(object : OperatingSystem by osImage {
            override val deadEndPattern: Regex get() = ".*$corruptingString.*".toRegex()
        }, osImage.credentials, osImage.file)

        expectCatching {
            println(corruptedOsImage)
            corruptedOsImage.execute(logger = logger)
        }
            .isFailure()
            .isA<ExecutionException>()
            .rootCause
            .isA<IllegalStateException>()
            .message
            .isNotNull()
            .get { logger.logged }
            .contains("Booting QEMU machine")
            .contains("The VM is stuck.")
    }

    @Suppress("unused")
    @AfterAll
    fun tearDown() {
        Docker.remove(name(::`should boot and run program in user session`), forcibly = true)
    }

    private fun name(test: KFunction<Any>) = DockerContainerName(OperatingSystemProcessTest::class.simpleName + "-" + test.name)
}
