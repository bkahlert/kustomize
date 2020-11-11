package com.imgcstmzr.runtime

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightMagenta
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.junit.assertTimeoutPreemptively
import com.bkahlert.koodies.tracing.trace
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.imgcstmzr.cli.TestCli
import com.imgcstmzr.runtime.OperatingSystems.Companion.Credentials
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.ProcessExitMock.Companion.computing
import com.imgcstmzr.runtime.ProcessExitMock.Companion.immediateSuccess
import com.imgcstmzr.runtime.ProcessMock.SlowInputStream.Companion.prompt
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.miniTrace
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.InMemoryLoggerFactory
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.minutes
import kotlin.time.seconds

@ExperimentalValueSourceApi
@Execution(CONCURRENT)
@OptIn(ExperimentalTime::class)
class OperatingSystemsTest {

    @Nested
    inner class Raspberry {
        val os = RaspberryPiLite

        @Test
        fun `should provide working url`() {
            expectThat(os.downloadUrl).isEqualTo("https://downloads.raspberrypi.org/raspios_lite_armhf_latest")
        }

        @ConcurrentTestFactory
        fun `should detect login line`() = mapOf(
            true to listOf(
                "raspberrypi login: ",
                "raspberry-pi login:",
                "DietPi login: ",
                "DietPi login:",
                "anything login:",
                "ignoreCASE login:",
                "trailingWhiteSpaces login: \t",
                "raspberry login: user input",
            ),
            false to listOf(
                "",
                "   ",
                "anything",
                "anything else:",
            )
        ).flatMap { (expected, values) ->
            values.map {
                dynamicTest(expected.asEmoji + " $it") {
                    expectThat(RaspberryPiLite.loginPattern.matches(it)).isEqualTo(expected)
                }
            }
        }

        @ConcurrentTestFactory
        fun `should detect ready line`() = mapOf(
            true to listOf(
                "pi@raspberrypi:~$",
                "pi@raspberry-pi:~$",
                "pi@raspberrypi:~$ ",
                "pi@raspberrypi:/etc$ \t",
                "root@raspberrypi:~#",
                "root@raspberrypi:/var/local# ",
                "someone@somewhere:/etc$ ",
                "SomeOne@SomewWere:/some/Dir$ ",
            ),
            false to listOf(
                "SomeOne@SomewWere:/some/Dir$: user input",
                "raspberrypi login:",
                "ignoreCASE login:",
                "trailingWhiteSpaces login: \t",
                "",
                "   ",
                "anything",
                "anything else:",
            )
        ).flatMap { (expected, values) ->
            values.map {
                dynamicTest(expected.asEmoji + " $it") {
                    expectThat(RaspberryPiLite.readyPattern.matches(it)).isEqualTo(expected)
                }
            }
        }

        @ConcurrentTestFactory
        fun `should detect dead end line`() = mapOf(
            true to listOf(
                "[ TIME ] Timed out waiting for device ",
                "[ TIME ] Timed out waiting for device /dev/serial1.  ",
                "*[ TIME ] Timed out waiting for device /dev/serial1.",
                "[ TIME ] Timed out waiting for device /dev/serial5.",
            ),
            false to listOf(
                "",
                "   ",
                "anything",
                "anything else:",
                "[ ON TIME ] Timed out waiting for device /dev/serial1.\r",
            )
        ).flatMap { (expected, values) ->
            values.map {
                dynamicTest(expected.asEmoji + " $it") {
                    expectThat(RaspberryPiLite.deadEndPattern.matches(it)).isEqualTo(expected)
                }
            }
        }

        @Test
        fun `should be provided with programs in correct order`() {
            TestCli.cmd.main(emptyList())
            val programs = TestCli.cmd.scripts

            expectThat(programs.keys).containsExactly(
                "the-basics",
                "very-----------------long",
                "middle",
                "s",
                "leisure")
        }

        @Slow
        @Execution(CONCURRENT)
        @TestFactory
        fun `should perform log in and terminate`(loggerFactory: InMemoryLoggerFactory<String?>): List<DynamicTest> = mapOf(
            "login:\\𝗻" to generateProcessOutput("\n"),
            "login:\\̵n̵" to generateProcessOutput(""),
        ).flatMap { (case: String, inputs: Array<Pair<Duration, String>>) ->
            val nonBlockingReaderTimeout = 100.milliseconds
            listOf(
                nonBlockingReaderTimeout / 2,
                nonBlockingReaderTimeout * 2,
            ).map { baseDelayPerWord ->
                val name = "$case + $baseDelayPerWord line delay"
                dynamicTest(name) {
                    val workflow = os.loginProgram(Credentials("john", "passwd123"))//.logging()
                    val logger = loggerFactory.createLogger(name)
                    val processMock = ProcessMock.withIndividuallySlowInput(
                        inputs = inputs,
                        baseDelayPerInput = baseDelayPerWord,
                        echoInput = true,
                        processExit = { immediateSuccess() },
                        logger = logger,
                    )
                    val runningOS = object : RunningOperatingSystem() {
                        override val logger: RenderingLogger<*> = logger
                        override var process: Process = processMock
                    }
                    val reader = NonBlockingReader(processMock.inputStream, timeout = nonBlockingReaderTimeout, logger = logger)

                    assertTimeoutPreemptively(2.minutes, {
                        var finished = false
                        reader.forEachLine { line ->
                            logger.miniTrace("read<<") {
                                if (finished) {
                                    trace(line.debug.magenta())
                                } else {
                                    trace(line.debug.brightMagenta())
                                    trace("... processing")
                                    finished = !workflow.compute(runningOS, OUT typed line)
                                    if (finished) {
                                        trace("FINISHED")
                                    } else {
                                        trace("Ongoing")
                                    }
                                }
                            }
                        }
                    }) {
                        logger.logResult { Result.failure(IllegalStateException("Deadlock")) }
                        META.format("Unprocessed output: ${processMock.inputStream}")
                    }

                    expectThat(workflow).get("Halted") { halted }.isTrue()
                    expectThat(processMock.received).contains("john").contains("passwd123").not { contains("stuff") }
                }
            }
        }

        @Test
        fun `should finish program`(logger: InMemoryLogger<String?>) {
            TestCli.cmd.main(emptyList())
            val scriptContent = TestCli.cmd.scripts["the-basics"] ?: throw NoSuchElementException("Could not load program")
            val script = os.compileSetupScript("test", scriptContent)[1]//.logging()
            val processMock = ProcessMock(logger = logger, processExit = { computing() })
            val runningOS = object : RunningOperatingSystem() {
                override val logger: RenderingLogger<*> = logger
                override var process: Process = processMock
            }

            assertTimeoutPreemptively(100.seconds, {
                var running = true
                while (running) {
                    running = script.compute(runningOS, OUT typed "pi@raspberrypi:~$ ")
                }
            }) { "Output till timeout: ${processMock.outputStream}" }

            expectThat(script).assertThat("Halted") { it.halted }
            expectThat(processMock.outputStream.toString().byteString()).isEqualTo("""
            sudo -i
            ${"\r"}
            echo "Configuring SSH port"
            sed -i 's/^\#Port 22${'$'}/Port 1234/g' /etc/ssh/sshd_config
            ${"\r"}
            exit
${"\r"}
        """.trimIndent().byteString())
        }

        private fun String.byteString() {
            toString().toByteArray().joinToString(", ") { "$it" }
        }
    }
}

@OptIn(ExperimentalTime::class)
val generateProcessOutput = { promptStart: String ->
    arrayOf(
        0.seconds to "boot\n",
        0.seconds to "boot\n",
        0.seconds to "boot\n",
        0.seconds to "raspberry-pi login: $promptStart", prompt(),
        2.seconds to "Password: $promptStart", prompt(),
        2.seconds to "lorem ipsum\n",
        0.seconds to "GNU\n",
        0.seconds to "...ABSOLUTELY NO WARRANTY...\n",
        0.seconds to "\n",
        0.seconds to "\n",
        0.seconds to "stuff\n",
        0.seconds to "juergen@raspberrypi:~$ \n",
    )
}
