package com.imgcstmzr.runtime

import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.imgcstmzr.cli.TestCli
import com.imgcstmzr.process.NonBlockingReader
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration


@ExperimentalValueSourceApi
@Execution(ExecutionMode.CONCURRENT)
class WellKnownOperatingSystemsTest {

    val os: OS = WellKnownOperatingSystems.RASPBERRY_PI_OS_LITE

    @Test
    internal fun `should provide working url`() {
        expectThat(os.downloadUrl).isEqualTo("downloads.raspberrypi.org/raspios_lite_armhf_latest")
    }

    @TestFactory
    internal fun `should detect login line`() = mapOf(
        true to listOf(
            "raspberrypi login: ",
            "raspberry-pi login:",
            "anything login:",
            "ignoreCASE login:",
            "trailingWhiteSpaces login: \t",
        ),
        false to listOf(
            "raspberry login: user input",
            "",
            "   ",
            "anything",
            "anything else:",
        )
    ).flatMap { (expected, values) ->
        values.map {
            dynamicTest((if (expected) "✅" else "❌") + " $it") {
                expectThat(WellKnownOperatingSystems.RASPBERRY_PI_OS_LITE.loginPattern.matches(it)).isEqualTo(expected)
            }
        }
    }

    @TestFactory
    internal fun `should detect ready line`() = mapOf(
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
            dynamicTest((if (expected) "✅" else "❌") + " $it") {
                expectThat(WellKnownOperatingSystems.RASPBERRY_PI_OS_LITE.readyPattern.matches(it)).isEqualTo(expected)
            }
        }
    }

    @Test
    internal fun `should be provided with programs in correct order`() {
        TestCli.cmd.main(emptyList())
        val programs = TestCli.cmd.commands

        expectThat(programs.keys).containsExactly(
            "the-basics",
            "very-----------------long",
            "middle",
            "shrt",
            "leisure")
    }

    @TestFactory
    internal fun `should perform log in and terminate`(): List<DynamicTest> {
        val maxWaitPerLine = 100.millis()
        val prompt = { promptStart: String ->
            arrayOf(
                "boot\n",
                "boot\n",
                "boot\n",
                "raspberry-pi login: $promptStart",
                "${maxWaitPerLine.plus(2.seconds())}",
                "Password: $promptStart",
                "${maxWaitPerLine.plus(2.seconds())}",
                "lorem ipsum\n",
                "GNU\n",
                "...ABSOLUTELY NO WARRANTY...\n",
                "\n",
                "\n",
                "stuff\n",
                "juergen@raspberrypi:~$ \n")
        }
        val promptWithLineBreaks = "prompt with new lines" to prompt("\n")
        val promptWithoutLineBreaks = "prompt without new lines" to prompt("")

        class Reader(val blocking: Boolean, val succeeding: Boolean = true) : (Process, (String) -> Unit) -> Unit {
            override fun invoke(process: Process, lineConsumer: (String) -> Unit) =
                if (blocking) BufferedReader(InputStreamReader(process.inputStream)).forEachLine(lineConsumer)
                else NonBlockingReader(process, timeout = maxWaitPerLine).forEachLine(lineConsumer)

            override fun toString(): String = (if (blocking) "" else "non-") + "blocking reader"
        }

        return mapOf(
//            Reader(blocking = true) to promptWithLineBreaks,
//            Reader(blocking = true, succeeding = false) to promptWithoutLineBreaks,
            Reader(blocking = false) to promptWithLineBreaks,
            Reader(blocking = false) to promptWithoutLineBreaks,
        ).flatMap { (reader, caseInput: Pair<String, Array<String>>) ->
            val case = caseInput.first
            val input = caseInput.second
            listOf(50.millis(), 200.millis()).map { delay ->
                dynamicTest("$reader processing $case with $delay delay per line") {
                    TestCli.cmd.main(emptyList())
                    val workflow = os.login("john", "passwd123").logging()
                    val fakeProcess = FakeProcess.withSlowInput(*input, delay = delay)

                    assertTimeoutPreemptively(Duration.ofSeconds(300), {
                        reader.invoke(fakeProcess) { line ->
                            TermUi.echo(line)
                            val finished = !workflow.process(fakeProcess, line)
                            if (finished) {
                                fakeProcess.exit(0)
                            }
                        }
                    }) { "Output till timeout: ${fakeProcess.output}" }

                    expectThat(workflow).assertThat("Halted") { it.halted == reader.succeeding }
                    expectThat(fakeProcess.output.toString()) {

                        if (reader.succeeding) contains("john").contains("passwd123").not { contains("stuff") }
                        else not { contains("john") }.not { contains("passwd123") }.not { contains("stuff") }
                    }
                }
            }
        }
    }

    @Test
    internal fun `should finish program`() {
        TestCli.cmd.main(emptyList())
        val program = TestCli.cmd.commands["the-basics"] ?: throw NoSuchElementException("Could not load program")
        val workflow = os.sequences("test", program)[1].logging()
        val fakeProcess = FakeProcess()

        assertTimeoutPreemptively(Duration.ofSeconds(10), {
            var running = true
            while (running) {
                running = workflow.process(fakeProcess, "pi@raspberrypi:~$ ")
            }
        }) { "Output till timeout: ${fakeProcess.output}" }

        expectThat(workflow).assertThat("Halted") { it.halted }
        expectThat(fakeProcess.output.toString().byteString()).isEqualTo("""
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
        toString().toByteArray().joinToString(", ") { it.toString() }
    }
}

private fun Int.seconds() = Duration.ofSeconds(this.toLong())
private fun Int.millis() = Duration.ofMillis(this.toLong())



