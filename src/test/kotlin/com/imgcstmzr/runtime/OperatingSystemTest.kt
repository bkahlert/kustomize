package com.imgcstmzr.runtime

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightMagenta
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.junit.assertTimeoutPreemptively
import com.bkahlert.koodies.time.sleep
import com.bkahlert.koodies.tracing.trace
import com.bkahlert.koodies.unit.Kibi
import com.bkahlert.koodies.unit.bytes
import com.imgcstmzr.cli.TestCli
import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.empty
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.ProcessExitMock.Companion.computing
import com.imgcstmzr.runtime.SlowInputStream.Companion.prompt
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.miniTrace
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.InMemoryLoggerFactory
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
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
    @ConcurrentTestFactory
    fun `should perform log in and terminate`(loggerFactory: InMemoryLoggerFactory<String?>): List<DynamicTest> = listOf(
        LoginSimulation(2.0, "\n"),
        LoginSimulation(0.5, "\n"),
        LoginSimulation(2.0, null),
        LoginSimulation(0.5, null),
    ).map { loginSimulation ->
        dynamicTest("$loginSimulation") {
            val processMock = loginSimulation.buildProcess(loggerFactory)
            val runningOS = processMock.start()

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
                    runningOS.logger.logResult { Result.failure(IllegalStateException("Deadlock")) }
                    META.format("Unprocessed output: ${runningOS.process.inputStream}")
                }
            }.onFailure { dump("Test failed.") { (processMock.logger as InMemoryLogger).logged } }

            expectThat(workflow).get("Halted") { halted }.isTrue()
            expectThat((runningOS.process as ProcessMock).received).contains("john").contains("passwd123").not { contains("stuff") }
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

    fun buildProcess(loggerFactory: InMemoryLoggerFactory<String?>): ProcessMock =
        ProcessMock.withIndividuallySlowInput(
            inputs = generateProcessOutput(promptTerminator),
            baseDelayPerInput = ioDelay,
            echoInput = true,
            processExit = { ProcessExitMock.immediateSuccess() },
            logger = loggerFactory.createLogger(toString()),
        )

    override fun toString(): String = "login" + (promptTerminator?.let { "\\n" } ?: "\\̵n") + "$ioDelay delay per I/O line"
}
