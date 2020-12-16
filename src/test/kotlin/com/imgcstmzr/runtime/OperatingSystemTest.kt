package com.imgcstmzr.runtime

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.regex.matchEntire
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightMagenta
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.junit.assertTimeoutPreemptively
import com.bkahlert.koodies.test.junit.test
import com.bkahlert.koodies.time.sleep
import com.bkahlert.koodies.tracing.trace
import com.bkahlert.koodies.unit.Kibi
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.runtime.JavaProcessMock.Companion.withIndividuallySlowInput
import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.empty
import com.imgcstmzr.runtime.SlowInputStream.Companion.prompt
import com.imgcstmzr.runtime.log.miniTrace
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.InMemoryLoggerFactory
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isTrue
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class OperatingSystemTest {

    private val os = object : OperatingSystem {
        override val fullName = "Test OS"
        override val name = "TestOS"
        override val downloadUrl = "https://some.where"
        override val approximateImageSize = 10.Kibi.bytes
        override val defaultCredentials = empty
    }

    @TestFactory
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
            "Last login: Sat Dec 12 12:39:17 GMT 2020 on ttyAMA0",
        )
    ).flatMap { (expected, values) ->
        values.map {
            dynamicTest(expected.asEmoji + " $it") {
                expectThat(OperatingSystem.DEFAULT_LOGIN_PATTERN).matchEntire(it, expected)
            }
        }
    }

    @TestFactory
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
            "FiFi@raspberrypi:~\$",
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
                expectThat(OperatingSystem.DEFAULT_READY_PATTERN).matchEntire(it, expected)
            }
        }
    }

    @TestFactory
    fun `should detect dead end line`() = mapOf(
        true to listOf(
            "You are in emergency mode. After logging in, type \"journalctl -xb\" to view",
            "You are in EMERGENCY MODE. After logging in, type \"journalctl -xb\" to view",
            "in emergency mode",
        ),
        false to listOf(
            "emergency",
            "",
            "   ",
            "anything",
            "anything else:",
        )
    ).flatMap { (expected, values) ->
        values.map {
            dynamicTest(expected.asEmoji + " $it") {
                expectThat(OperatingSystem.DEFAULT_DEAD_END_PATTERN).matchEntire(it, expected)
            }
        }
    }

    @Slow
    @TestFactory
    fun `should perform log in and terminate`(loggerFactory: InMemoryLoggerFactory): List<DynamicTest> = listOf(
        LoginSimulation(2.0, "\n"),
        LoginSimulation(0.5, "\n"),
        LoginSimulation(2.0, null),
        LoginSimulation(0.5, null),
    ).test("{}") { loginSimulation ->
        val processMock = loginSimulation.buildProcess(loggerFactory)
        val runningOS = processMock.start(loginSimulation.toString())

        val reader = NonBlockingReader(processMock.inputStream, timeout = loginSimulation.readerTimeout, logger = processMock.logger)

        val workflow = os.loginProgram(Credentials("john", "passwd123"))//.logging()
        kotlin.runCatching {
            assertTimeoutPreemptively(45.seconds, {
                var finished = false
                reader.forEachLine { line ->
                    runningOS.logger.miniTrace("read<<") {
                        if (finished) {
                            trace(line.debug.magenta())
                        } else {
                            trace(line.debug.brightMagenta())
                            trace("... processing")
                            500.milliseconds.sleep()
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
                runningOS.logger.logResult<Any> { Result.failure(IllegalStateException("Deadlock")) }
                META.format("Unprocessed output: ${runningOS.inputStream}")
            }
        }.onFailure { dump("Test failed.") { (processMock.logger as InMemoryLogger).logged } }

        expectThat(workflow).get("Halted") { halted }.isTrue()
        expectThat(processMock.received).contains("john").contains("passwd123").not { contains("stuff") }
    }
}

data class LoginSimulation(val readerTimeout: Duration, val ioDelay: Duration, val promptTerminator: String?) {
    constructor(ioDelayFactor: Double, promptTerminator: String?) : this(100.milliseconds, 100.milliseconds * ioDelayFactor, promptTerminator)

    private fun generateProcessOutput(promptLineSeparator: String?): Array<Pair<Duration, String>> = arrayOf(
        0.seconds to "boot\n",
        0.seconds to "boot\n",
        0.seconds to "boot\n",
        0.seconds to "raspberry-pi login: ${promptLineSeparator ?: ""}", prompt(),
        2.seconds to "Password: ${promptLineSeparator ?: ""}", prompt(),
        2.seconds to "lorem ipsum\n",
        0.seconds to "GNU\n",
        0.seconds to "...ABSOLUTELY NO WARRANTY...\n",
        0.seconds to "\n",
        0.seconds to "\n",
        0.seconds to "stuff\n",
        0.seconds to "juergen@raspberrypi:~$ \n",
    )

    fun buildProcess(loggerFactory: InMemoryLoggerFactory): JavaProcessMock =
        with(loggerFactory.createLogger(toString())) {
            withIndividuallySlowInput(
                inputs = generateProcessOutput(promptTerminator),
                baseDelayPerInput = ioDelay,
                echoInput = true,
                processExit = { ProcessExitMock.immediateSuccess() },
            )
        }

    override fun toString(): String = "login" + (promptTerminator?.let { "\\n" } ?: "\\̵n") + "$ioDelay delay per I/O line"
}
