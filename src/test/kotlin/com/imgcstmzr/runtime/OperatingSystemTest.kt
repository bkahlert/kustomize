package com.imgcstmzr.runtime

import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.empty
import com.imgcstmzr.test.Slow
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.assertTimeoutPreemptively
import com.imgcstmzr.test.logging.InMemoryLoggerFactory
import com.imgcstmzr.test.matchEntire
import com.imgcstmzr.testWithTempDir
import koodies.concurrent.process.IO.Type.META
import koodies.concurrent.process.IO.Type.OUT
import koodies.concurrent.process.process
import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exception.dump
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.process.JavaProcessMock
import koodies.process.JavaProcessMock.Companion.withIndividuallySlowInput
import koodies.process.ProcessExitMock
import koodies.process.SlowInputStream.Companion.prompt
import koodies.terminal.AnsiColors.brightMagenta
import koodies.terminal.AnsiColors.magenta
import koodies.time.sleep
import koodies.tracing.miniTrace
import koodies.unit.Kibi
import koodies.unit.bytes
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
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
    fun `should perform log in and terminate`(loggerFactory: InMemoryLoggerFactory, uniqueId: UniqueId): List<DynamicTest> = listOf(
        LoginSimulation(2.0, "\n"),
        LoginSimulation(0.5, "\n"),
        LoginSimulation(2.0, null),
        LoginSimulation(0.5, null),
    ).testWithTempDir(uniqueId, "{}") { loginSimulation ->
        val processMock: JavaProcessMock = loginSimulation.buildProcess(loggerFactory)
        val runningOS = OperatingSystemProcessMock(loginSimulation.toString(), processMock.start(loginSimulation.toString()))

        val reader = NonBlockingReader(processMock.inputStream, timeout = loginSimulation.readerTimeout, logger = processMock.logger)

        val program = os.loginProgram(Credentials("john", "passwd123"))//.logging()
        kotlin.runCatching {
            assertTimeoutPreemptively(45.seconds, {
                var finished = false
                reader.forEachLine { line ->
                    runningOS.logger.miniTrace("read<<") {
                        if (finished) {
                            this?.trace(line.debug.magenta())
                        } else {
                            this?.trace(line.debug.brightMagenta())
                            this?.trace("... processing")
                            500.milliseconds.sleep()
                            finished = !program.compute(runningOS, OUT typed line)
                            if (finished) {
                                this?.trace("FINISHED")
                            } else {
                                this?.trace("Ongoing")
                            }
                        }
                    }
                }
            }) {
                runningOS.logger.logResult<Any> { Result.failure(IllegalStateException("Deadlock")) }
                META.format("Unprocessed output: ${runningOS.inputStream}")
            }
        }.onFailure { dump("Test failed.") { (processMock.logger as InMemoryLogger).logged } }

        expectThat(program).get("Halted") { halted }.isTrue()
        expectThat(processMock.received).contains("john").contains("passwd123").not { contains("stuff") }
    }

    @Slow @Test
    fun InMemoryLogger.`should invoke shutdown even if not ready`(uniqueId: UniqueId) {
        val process = withIndividuallySlowInput(
            0.milliseconds to "[  OK  ] Started Update UTMP about System Runlevel Changes.\n",
            prompt(),
            100.milliseconds to "Shutting down",
            baseDelayPerInput = 100.milliseconds,
            processExit = {
                object : ProcessExitMock(0, Duration.ZERO) {
                    override fun invoke(): Int {
                        while (!outputStream.toString().contains(os.shutdownCommand)) {
                            100.milliseconds.sleep()
                        }
                        return 0
                    }

                    override fun invoke(timeout: Duration): Boolean {
                        while (!outputStream.toString().contains(os.shutdownCommand)) {
                            100.milliseconds.sleep()
                        }
                        return true
                    }
                }
            },
            echoInput = true)

        val runningOS = OperatingSystemProcessMock(uniqueId.simple, process.start(uniqueId.simple))
        val shutdownProgram = os.shutdownProgram()

        val exitValue = runningOS.process(nonBlockingReader = false) { io ->
            shutdownProgram.compute(runningOS, io)
        }.waitForTermination()

        expectThat(exitValue) {
            isEqualTo(0)
        }
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
                baseDelayPerInput = ioDelay,
                echoInput = true,
                processExit = { ProcessExitMock.immediateSuccess() },
                inputs = generateProcessOutput(promptTerminator),
            )
        }

    override fun toString(): String = "login" + (promptTerminator?.let { "\\n" } ?: "\\̵n") + "$ioDelay delay per I/O line"
}
