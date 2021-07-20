package com.imgcstmzr.os

import com.imgcstmzr.os.OperatingSystem.Credentials
import com.imgcstmzr.os.OperatingSystem.Credentials.Companion.empty
import koodies.debug.debug
import koodies.docker.isSuccessful
import koodies.exec.IO
import koodies.exec.ProcessingMode
import koodies.exec.ProcessingMode.Interactivity.Interactive
import koodies.exec.ProcessingMode.Synchronicity.Async
import koodies.exec.mock.JavaProcessMock
import koodies.exec.mock.JavaProcessMock.Companion.withIndividuallySlowInput
import koodies.exec.mock.SlowInputStream.Companion.prompt
import koodies.exec.process
import koodies.junit.TestName
import koodies.junit.UniqueId
import koodies.nio.NonBlockingReader
import koodies.test.Slow
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.ANSI.Text.Companion.ansi
import koodies.time.seconds
import koodies.time.sleep
import koodies.tracing.rendering.spanningLine
import koodies.tracing.spanning
import koodies.unit.Kibi
import koodies.unit.bytes
import koodies.unit.milli
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isTrue
import strikt.assertions.matches
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
        "wifi3--pUiy login:",
        "wifi3--pUiy login: Starting Update UTMP about System Runlevel Changes...",
        "wifi3--fKwh login: [  OK  ] Started Regenerate SSH host keys.",
        "wifi3--fKwh login: [  OK  ] Started Update UTMP about System Runlevel Changes.",
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
    fun `should detect password line`() = testEach(
        "Password: ",
        "Password:",
        "password: ",
        "Password: Starting Update UTMP about System Runlevel Changes...",
    ) { asserting { matches(OperatingSystem.DEFAULT_PASSWORD_PATTERN) } }

    @TestFactory
    fun `should detect non-password line`() = testEach(
        "",
        "   ",
        "Password-",
        "Enter Password: ",
    ) { asserting { not { matches(OperatingSystem.DEFAULT_PASSWORD_PATTERN) } } }

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
        "john.doe@raspberrypi:~$",
        "john.doe@demo--rx1E:~$",
        "john.doe@demo--rx1E:~$ ",
        "pi@raspberrypi:~$ Starting Update UTMP about System Runlevel Changes...",
        "SomeOne@SomewWere:/some/Dir$: user input",
    ) { asserting { matches(OperatingSystem.DEFAULT_READY_PATTERN) } }

    @TestFactory
    fun `should detect non-ready line`() = testEach(
        "raspberrypi login:",
        "ignoreCASE login:",
        "trailingWhiteSpaces login: \t",
        "",
        "   ",
        "anything",
        "anything else:",
    ) { asserting { not { matches(OperatingSystem.DEFAULT_READY_PATTERN) } } }

    @TestFactory
    @Suppress("SpellCheckingInspection")
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
    fun `should perform log in and terminate`(uniqueId: UniqueId) = testEach(
        LoginSimulation(2.0, "\n"),
        LoginSimulation(0.5, "\n"),
        LoginSimulation(2.0, null),
        LoginSimulation(0.5, null),
    ) { loginSimulation ->
        withTempDir(uniqueId) {
            expecting {
                val processMock: JavaProcessMock = loginSimulation.buildProcess()
                spanning(loginSimulation.toString()) {
                    val runningOS = OperatingSystemProcessMock(
                        loginSimulation.toString(),
                        processMock.start(loginSimulation.toString()),
                        this,
                    )

                    val reader =
                        NonBlockingReader(processMock.inputStream, timeout = loginSimulation.readerTimeout)

                    val program = os.loginProgram(Credentials("john", "passwd123"))//.logging()

                    var finished = false
                    reader.forEachLine { line ->
                        spanningLine("read line") {
                            if (finished) {
                                log(line.debug.ansi.green)
                            } else {
                                log(line.debug.ansi.brightGreen)
                                log("processing")
                                500.milli.seconds.sleep()
                                finished = !program.compute(runningOS, IO.Output typed line)
                                if (finished) {
                                    log("FINISHED")
                                } else {
                                    log("ongoing")
                                }
                            }
                        }
                    }

                    program
                }
            } that {
                get("Halted") { halted }.isTrue()
            }
        }
    }

    @Slow @Test
    @Suppress("SpellCheckingInspection")
    fun `should invoke shutdown even if not ready`(testName: TestName, uniqueId: UniqueId) = spanning(testName) {
        val process = withIndividuallySlowInput(
            0.milli.seconds to "[  OK  ] Started Update UTMP about System Runlevel Changes.\n",
            prompt(),
            100.milli.seconds to "Shutting down",
            echoInput = true,
            baseDelayPerInput = 100.milli.seconds,
        ) {
            while (!outputStream.toString().contains(os.shutdownCommand.shellCommand)) {
                100.milli.seconds.sleep()
            }
            0
        }

        val runningOS = OperatingSystemProcessMock(uniqueId.value, process.start(uniqueId.value), this)
        val shutdownProgram = os.shutdownProgram().logging()

        expecting {
            runningOS.process(ProcessingMode(Async, Interactive(false))) { _, process ->
                process { io -> shutdownProgram.compute(runningOS, io) }
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
        0.seconds to "john@raspberrypi:~$ \n",
    )

    fun buildProcess(): JavaProcessMock =
        withIndividuallySlowInput(
            baseDelayPerInput = ioDelay,
            echoInput = false,
            inputs = generateProcessOutput(promptTerminator),
        )

    override fun toString(): String = "login" + (promptTerminator?.let { "\\n" } ?: "\\̵n̵") + " with $ioDelay delay per I/O line"
}
