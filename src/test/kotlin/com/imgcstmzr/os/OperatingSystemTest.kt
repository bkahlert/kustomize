package com.imgcstmzr.os

import com.imgcstmzr.os.OperatingSystem.Credentials
import com.imgcstmzr.os.OperatingSystem.Credentials.Companion.empty
import koodies.debug.debug
import koodies.docker.isSuccessful
import koodies.exec.IO
import koodies.exec.ProcessingMode
import koodies.exec.ProcessingMode.Interactivity.Interactive
import koodies.exec.ProcessingMode.Synchronicity.Sync
import koodies.exec.mock.JavaProcessMock
import koodies.exec.mock.JavaProcessMock.Companion.withIndividuallySlowInput
import koodies.exec.mock.SlowInputStream.Companion.prompt
import koodies.exec.process
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.assertTimeoutPreemptively
import koodies.test.expecting
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.ANSI.Text.Companion.ansi
import koodies.time.sleep
import koodies.unit.Kibi
import koodies.unit.bytes
import koodies.unit.milli
import koodies.time.seconds
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isTrue
import strikt.assertions.matches
import strikt.assertions.second
import kotlin.time.Duration

class OperatingSystemTest {

    private val os = object : OperatingSystem {
        override val fullName = "Test OS"
        override val name = "TestOS"
        override val downloadUrl = "https://some.where"
        override val approximateImageSize = 10.Kibi.bytes
        override val defaultCredentials = empty
    }

    @TestFactory
    fun `should detect login line`() = testEach(
        "raspberrypi login: ",
        "raspberry-pi login:",
        "DietPi login: ",
        "DietPi login:",
        "anything login:",
        "ignoreCASE login:",
        "trailingWhiteSpaces login: \t",
        "raspberry login: user input",
    ) { asserting { matches(OperatingSystem.DEFAULT_LOGIN_PATTERN) } }

    @TestFactory
    fun `should detect non-login line`() = testEach(
        "",
        "   ",
        "anything",
        "anything else:",
        "Last login: Sat Dec 12 12:39:17 GMT 2020 on ttyAMA0",
    ) { asserting { not { matches(OperatingSystem.DEFAULT_LOGIN_PATTERN) } } }

    @TestFactory
    fun `should detect ready line`() = testEach(
        "pi@raspberrypi:~$",
        "pi@raspberry-pi:~$",
        "pi@raspberrypi:~$ ",
        "pi@raspberrypi:/etc$ \t",
        "root@raspberrypi:~#",
        "root@raspberrypi:/var/local# ",
        "someone@somewhere:/etc$ ",
        "SomeOne@SomewWere:/some/Dir$ ",
        "FiFi@raspberrypi:~\$",
    ) { asserting { matches(OperatingSystem.DEFAULT_READY_PATTERN) } }

    @TestFactory
    fun `should detect non-ready line`() = testEach(
        "SomeOne@SomewWere:/some/Dir$: user input",
        "raspberrypi login:",
        "ignoreCASE login:",
        "trailingWhiteSpaces login: \t",
        "",
        "   ",
        "anything",
        "anything else:",
    ) { asserting { not { matches(OperatingSystem.DEFAULT_READY_PATTERN) } } }

    @TestFactory
    fun `should detect dead end line`() = testEach(
        "You are in emergency mode. After logging in, type \"journalctl -xb\" to view",
        "You are in EMERGENCY MODE. After logging in, type \"journalctl -xb\" to view",
        "in emergency mode",
    ) { asserting { matches(OperatingSystem.DEFAULT_DEAD_END_PATTERN) } }

    @TestFactory
    fun `should detect non-dead end line`() = testEach(
        "emergency",
        "",
        "   ",
        "anything",
        "anything else:",
    ) { asserting { not { matches(OperatingSystem.DEFAULT_DEAD_END_PATTERN) } } }

    @Slow
    @TestFactory
    fun `should perform log in and terminate`(loggerFactory: InMemoryLoggerFactory, uniqueId: UniqueId) = testEach(
        LoginSimulation(2.0, "\n"),
        LoginSimulation(0.5, "\n"),
        LoginSimulation(2.0, null),
        LoginSimulation(0.5, null),
    ) { loginSimulation ->
        withTempDir(uniqueId) {
            expecting {
                val processMock: JavaProcessMock = loginSimulation.buildProcess(loggerFactory)
                val runningOS = OperatingSystemProcessMock(loginSimulation.toString(),
                    processMock.start(loginSimulation.toString()),
                    processMock.logger)

                val reader =
                    NonBlockingReader(processMock.inputStream, timeout = loginSimulation.readerTimeout, logger = processMock.logger)

                val program = os.loginProgram(Credentials("john", "passwd123"))//.logging()

                assertTimeoutPreemptively(45.seconds, {
                    var finished = false
                    reader.forEachLine { line ->
                        runningOS.logger.compactLogging("read<<") {
                            if (finished) {
                                logLine { line.debug.ansi.green }
                            } else {
                                logLine { line.debug.ansi.brightGreen }
                                logLine { "… processing" }
                                500.milli.seconds.sleep()
                                finished = !program.compute(runningOS, IO.Output typed line)
                                if (finished) {
                                    logLine { "FINISHED" }
                                } else {
                                    logLine { "Ongoing" }
                                }
                            }
                        }
                    }
                }) {
                    runningOS.logger.logResult<Any> { Result.failure(IllegalStateException("Deadlock")) }
                    IO.Meta typed "Unprocessed output: ${runningOS.inputStream}"
                }
                processMock to program
            } that {
                second.get("Halted") { halted }.isTrue()
            }
        }
    }

    @Slow @Test
    fun InMemoryLogger.`should invoke shutdown even if not ready`(uniqueId: UniqueId) {
        val process = withIndividuallySlowInput(
            0.milli.seconds to "[  OK  ] Started Update UTMP about System Runlevel Changes.\n",
            prompt(),
            100.milli.seconds to "Shutting down",
            echoInput = true,
            baseDelayPerInput = 100.milli.seconds,
        ) {
            while (!outputStream.toString().contains(os.shutdownCommand)) {
                100.milli.seconds.sleep()
            }
            0
        }

        val runningOS = OperatingSystemProcessMock(uniqueId.value, process.start(uniqueId.value), this)
        val shutdownProgram = os.shutdownProgram()

        expecting {
            runningOS.process(ProcessingMode(Sync, Interactive(false))) { io ->
                shutdownProgram.compute(runningOS, io)
            }.waitFor()
        } that {
            isSuccessful()
        }
    }
}

data class LoginSimulation(val readerTimeout: Duration, val ioDelay: Duration, val promptTerminator: String?) {
    constructor(ioDelayFactor: Double, promptTerminator: String?) : this(100.milli.seconds, 100.milli.seconds * ioDelayFactor, promptTerminator)

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
                echoInput = false,
                inputs = generateProcessOutput(promptTerminator),
            )
        }

    override fun toString(): String = "login" + (promptTerminator?.let { "\\n" } ?: "\\̵n") + "$ioDelay delay per I/O line"
}
