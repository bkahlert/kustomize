package com.imgcstmzr.runtime

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.runtime.ArmRunner.runOn
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.OperatingSystems.TinyCore
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.message
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import kotlin.reflect.KFunction
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.minutes
import kotlin.time.seconds

@DockerRequired
@Execution(CONCURRENT)
@OptIn(ExperimentalTime::class)
class ArmRunnerTest {

    @Test
    fun `should boot`(@OS(DietPi::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val bootedPatterns = listOf(osImage.loginPattern, osImage.passwordPattern, osImage.readyPattern)
        var booted = false

        var process: DockerProcess? = null
        kotlin.runCatching {
            process = ArmRunner.run(
                name = name(test = ArmRunnerTest::`should boot`),
                osImage = osImage,
                logger = logger, outputProcessor = { output ->
                    logger.logLine { output.formatted }
                    if (bootedPatterns.any { output.unformatted.matches(it) }) {
                        booted = true
                    }
                })

            val startTime = System.currentTimeMillis()
            while ((System.currentTimeMillis() - startTime).milliseconds < 10.minutes) {
                1.seconds.sleep()
                if (booted) break
            }

            expectThat(booted).isTrue()
            process?.destroyForcibly()
        }.onFailure { process?.destroyForcibly() }.getOrThrow()
    }

    @Test
    fun `should boot and run program in user session`(@OS(RaspberryPiLite::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {

        val exitCode = ArmRunner.run(
            name = name(ArmRunnerTest::`should boot and run program in user session`),
            osImage = osImage,
            logger = logger,
            programs = arrayOf(
                osImage.compileScript("demo train", "sudo apt-get install -y -m sl", "sl"),
            ))

        expect {
            that(exitCode).isEqualTo(0)
            that(logger.logged) {
                compose("did run sl or at least tried (in case of internet problems)") {
                    contains("Building dependency tree")
                    contains("@@(@@@)")
                } then {
                    if (anyPassed) pass()
                    else fail("neither tried nor succeeded")
                }
                get { lines().takeLast(20) }
                    .any { contains("shutting down") }
                    .any { contains("reboot: System halted") }
            }
        }
    }

    @Test
    fun `should run program in user session`(@OS(TinyCore::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {

        osImage.compileScript("demo train", "sudo apt-get install -y -m sl", "sl").runOn(osImage, logger)

        expectThat(logger.logged) {
            contains("Running Tiny Core at piCore-12.0.img with ◀◀ demo train")
            contains("Booting QEMU machine")
        }
    }

    @Test
    fun `should terminate if obviously stuck`(@OS(TinyCore::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val corruptingString = "Booting Linux"
        val corruptedOsImage = OperatingSystemImage(object : OperatingSystem by osImage {
            override val deadEndPattern: Regex get() = ".*$corruptingString.*".toRegex()
        }, osImage.credentials, Path.of(osImage.path))

        expectCatching { corruptedOsImage.boot(logger) }
            .isFailure()
            .isA<ExecutionException>()
            .rootCause
            .isA<IllegalStateException>()
            .message
            .isNotNull()
            .contains(corruptingString)
            .get { logger.logged }
            .contains("Booting QEMU machine")
            .contains("The VM is stuck.")
            .not { contains("Running Tiny Core at piCore-12.0.img with ◀◀ demo train") }
    }

    @Suppress("unused")
    @AfterAll
    fun tearDown() {
        Docker.remove(name(ArmRunnerTest::`should boot`), forcibly = true)
        Docker.remove(name(ArmRunnerTest::`should boot and run program in user session`), forcibly = true)
    }

    private fun name(test: KFunction<Any>) = ArmRunnerTest::class.simpleName + "-" + test.name
}
