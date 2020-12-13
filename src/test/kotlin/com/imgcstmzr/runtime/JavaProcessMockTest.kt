package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.startAsDaemon
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.string.joinLinesToString
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.junit.assertTimeoutPreemptively
import com.bkahlert.koodies.test.junit.test
import com.imgcstmzr.runtime.JavaProcessMock.Companion.processMock
import com.imgcstmzr.runtime.JavaProcessMock.Companion.withIndividuallySlowInput
import com.imgcstmzr.runtime.JavaProcessMock.Companion.withSlowInput
import com.imgcstmzr.runtime.ProcessExitMock.Companion.immediateExit
import com.imgcstmzr.runtime.ProcessExitMock.Companion.immediateSuccess
import com.imgcstmzr.runtime.SlowInputStream.Companion.prompt
import com.imgcstmzr.runtime.SlowInputStream.Companion.slowInputStream
import com.imgcstmzr.runtime.log.subTrace
import com.imgcstmzr.util.isEqualToByteWise
import com.imgcstmzr.util.logging.InMemoryLogger
import org.apache.commons.io.output.ByteArrayOutputStream
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isTrue
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class JavaProcessMockTest {

    @Nested
    inner class WithSlowInputStream {
        @Test
        fun InMemoryLogger.`should provide input correctly`() {
            val slowInputStream = slowInputStream("Hello\n", "World!\n", baseDelayPerInput = 1.seconds)

            assertTimeoutPreemptively(10.seconds) {
                val read = String(slowInputStream.readBytes())

                expectThat(read).isEqualTo("Hello\nWorld!\n")
            }
        }

        @Test
        fun InMemoryLogger.`should provide input slowly`() {
            val delay = 1.seconds
            val slowInputStream = slowInputStream("Hello\n", "World!\n", baseDelayPerInput = delay)

            assertTimeoutPreemptively(delay * 5) {
                val duration = measureTime {
                    String(slowInputStream.readBytes())
                }
                expectThat(duration).assertThat("is slow") { it > delay }
            }
        }

        @TestFactory
        fun InMemoryLogger.`should provide 'block on prompt' behavior`() = listOf(
            "with echoed input" to true,
            "without echoed input" to false,
        ).test("{}") { (name, echoOption) ->
            val byteArrayOutputStream = ByteArrayOutputStream()
            val slowInputStream = slowInputStream(
                0.seconds to "Password? ",
                prompt(),
                0.seconds to "\r",
                0.seconds to "Correct!\n",
                baseDelayPerInput = 0.seconds,
                echoInput = echoOption,
                byteArrayOutputStream = byteArrayOutputStream,
            )

            val input = "password1234"
            val output = StringBuilder()
            val start = System.currentTimeMillis()
            while (!slowInputStream.terminated) {
                if ((System.currentTimeMillis() - start).milliseconds > .8.seconds) {
                    byteArrayOutputStream.write("password1234\r".toByteArray())
                    byteArrayOutputStream.flush()
                }
                val available = slowInputStream.available()
                if (available > 0) {
                    val byteArray = ByteArray(available)
                    val read = slowInputStream.read(byteArray, 0, available)
                    expectThat(read).isGreaterThan(0)
                    output.append(String(byteArray))
                }
                Thread.sleep(10)
            }
            if (echoOption) expectThat(output).isEqualToByteWise("Password? $input\r\rCorrect!\n")
            else expectThat(output).isEqualToByteWise("Password? \rCorrect!\n")
        }


        @Test
        fun InMemoryLogger.`should produce same byte sequence as ByteArrayInputStream`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val inputStream = slowInputStream(input, baseDelayPerInput = 2.seconds)
            expectThat(input.byteInputStream().readAllBytes()).isEqualTo(inputStream.readAllBytes())
        }

        @TestFactory
        fun InMemoryLogger.`should never apply delay at at end stream`() = "𝌪".let { input ->
            listOf(
                slowInputStream(input,
                    baseDelayPerInput = 5.seconds,
                    echoInput = true),
                slowInputStream(0.seconds to input,
                    baseDelayPerInput = 5.seconds,
                    echoInput = true),
            ).test("{}") { inputStream ->
                val duration = measureTime {
                    @Suppress("ControlFlowWithEmptyBody")
                    while (inputStream.read() > -1) {
                    }
                    inputStream.available()
                    inputStream.read()
                    inputStream.available()
                    inputStream.read()
                    inputStream.available()
                    inputStream.read()
                }
                expectThat(duration).isLessThanOrEqualTo(2.seconds)
            }
        }
    }

    @Nested
    inner class ReadingExitValue {
        val expectedExitValue = 42

        @Nested
        inner class UsingExitValue {
            @Test
            fun InMemoryLogger.`should return mocked exit`() {
                val p = processMock(processExit = { immediateExit(expectedExitValue) })
                expectThat(p.exitValue()).isEqualTo(expectedExitValue)
            }

            @Test
            fun InMemoryLogger.`should throw on exception`() {
                val p = processMock(processExit = { throw IllegalStateException() })

                expectCatching {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    p.exitValue()
                }.isFailure().isA<IllegalStateException>()
            }
        }

        @Slow
        @Isolated // benchmark
        @Nested
        inner class UsingWaitFor {
            @Test
            fun InMemoryLogger.`should return mocked exit code`() {
                val p = processMock(processExit = { immediateExit(expectedExitValue) })

                val exitValue = p.waitFor()

                expectThat(exitValue).isEqualTo(expectedExitValue)
            }

            @Test
            fun InMemoryLogger.`should throw on exception`() {
                val p = processMock(processExit = { throw IllegalStateException() })

                expectCatching {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    p.waitFor()
                }.isFailure().isA<IllegalStateException>()
            }

            @Test
            fun InMemoryLogger.`should delay exit`() {
                val p = processMock(processExit = { ProcessExitMock.delayedExit(expectedExitValue, 50.milliseconds) })
                val start = System.currentTimeMillis()

                val exitValue = p.waitFor()

                expectThat(exitValue).isEqualTo(42)
                expectThat(System.currentTimeMillis() - start).isGreaterThan(40).isLessThan(80)
            }

            @Test
            fun InMemoryLogger.`should return true if process exits in time`() {
                val p = processMock(processExit = { ProcessExitMock.delayedExit(expectedExitValue, 50.milliseconds) })

                val returnValue = p.waitFor(100, TimeUnit.MILLISECONDS)

                expectThat(returnValue).isTrue()
            }

            @Test
            fun InMemoryLogger.`should return false if process not exits in time`() {
                val p = processMock(processExit = { ProcessExitMock.delayedExit(expectedExitValue, 50.milliseconds) })

                val returnValue = p.waitFor(25, TimeUnit.MILLISECONDS)

                expectThat(returnValue).isTrue()
            }
        }

        @Nested
        inner class Liveliness {

            @Nested
            inner class WithDefaultInputStream {
                @Test
                fun InMemoryLogger.`should be finished if exit is immediate`() {
                    val p = processMock(processExit = { immediateExit(expectedExitValue) })
                    expectThat(p.isAlive).isFalse()
                }

                @Test
                fun InMemoryLogger.`should be alive if exit is delayed`() {
                    val p = processMock(processExit = { ProcessExitMock.delayedExit(expectedExitValue, 50.milliseconds) })
                    expectThat(p.isAlive).isTrue()
                }
            }

            @Nested
            inner class WithSlowInputStream {
                @Test
                fun InMemoryLogger.`should be finished if all read`() {
                    val p = withSlowInput(echoInput = true, processExit = { immediateExit(expectedExitValue) })
                    expectThat(p.isAlive).isFalse()
                }

                @Test
                fun InMemoryLogger.`should be alive if not all read`() {
                    val p = withSlowInput("unread",
                        echoInput = true,
                        processExit = { immediateExit(expectedExitValue) })
                    expectThat(p.isAlive).isTrue()
                }
            }
        }
    }

    @Nested
    inner class OutputStreamWiring {
        @Test
        fun InMemoryLogger.`should allow SlowInputStream to read process's input stream`() {
            val p = withIndividuallySlowInput(prompt(), processExit = { immediateSuccess() }, echoInput = true)
            with(p.outputStream.writer()) {
                expectThat(p.received).isEmpty()
                expectThat(p.inputStream.available()).isEqualTo(0)

                write("user input")
                flush() // !

                expectThat(p.received).isEqualTo("user input")
                subTrace<Any?>("???") {
                    (p.inputStream as SlowInputStream).processInput(this)
                }
                expectThat(p.inputStream.available()).isEqualTo("user input".length)
            }
        }
    }

    @Slow
    @Test
    fun InMemoryLogger.`should terminate if all output is manually read`() {
        val p = withIndividuallySlowInput(
            500.milliseconds to "Welcome!\n",
            500.milliseconds to "Password? ",
            prompt(),
            500.milliseconds to "\r",
            500.milliseconds to "Correct!\n",
            baseDelayPerInput = 1.seconds,
            echoInput = true,
            processExit = { immediateSuccess() },
        )

        val reader = NonBlockingReader(p.inputStream, logger = this)

        startAsDaemon {
            Thread.sleep(5000)
            p.outputStream.enter("password")
        }

        expectThat(reader.readLine()).isEqualTo("Welcome!")
        expectThat(reader.readLine()).isEqualTo("Password? password")
        expectThat(reader.readLine()).isEqualTo("")
        expectThat(reader.readLine()).isEqualTo("Correct!")
        expectThat(reader.readLine()).isEqualTo(null)
        expectThat(p.isAlive).isFalse()
    }

    @Slow
    @Test
    fun InMemoryLogger.`should terminate if all output is consumed`() {
        val p = withIndividuallySlowInput(
            500.milliseconds to "Welcome!\n",
            500.milliseconds to "Password? ",
            prompt(),
            500.milliseconds to "\r",
            500.milliseconds to "Correct!\n",
            baseDelayPerInput = 1.seconds,
            echoInput = true,
            processExit = { immediateSuccess() },
        )

        val reader = NonBlockingReader(p.inputStream, logger = this)

        startAsDaemon {
            Thread.sleep(5000)
            p.outputStream.enter("password1234")
        }

        expectThat(reader.readLines().joinLinesToString()).isEqualToByteWise("""
            Welcome!
            Password? password1234
            
            Correct!
        
        """.trimIndent())
        expectThat(p.isAlive).isFalse()
    }

    @Test
    fun InMemoryLogger.`should provide access to unfiltered output stream`() {
        val p = withIndividuallySlowInput(
            baseDelayPerInput = 1.seconds,
            echoInput = true,
            processExit = { immediateSuccess() },
        )

        p.outputStream.write("Test1234\r".toByteArray())
        p.outputStream.write("Just in case\n".toByteArray())
        p.outputStream.flush()

        (p.inputStream as SlowInputStream).available()
        expectThat(p.received).isEqualTo("Test1234\rJust in case\n")
    }
}
