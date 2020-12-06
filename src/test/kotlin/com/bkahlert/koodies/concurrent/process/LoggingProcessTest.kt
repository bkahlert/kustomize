@file:Suppress("FINAL_UPPER_BOUND", "unused", "BlockingMethodInNonBlockingContext")

package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.junit.test
import com.bkahlert.koodies.test.strikt.containsExactlyInSomeOrder
import com.bkahlert.koodies.time.IntervalPolling
import com.bkahlert.koodies.time.poll
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.cause
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isTrue
import strikt.assertions.message
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class LoggingProcessTest {

    @Nested
    inner class Startup {

        @Test
        fun `should be alive`() {
            val process = createCompletingLoggingProcess(sleep = 5.seconds)
            expectThat(process).isAlive()
            process.destroyForcibly()
        }

        @Test
        fun `should meta log documents`() {
            val process = createCompletingLoggingProcess()
            expectThat(process).log.logs {
                any {
                    it.contains("📄")
                    it.contains("file:")
                    it.contains(".sh")
                }
            }

        }

        @Test
        fun `should provide PID`() {
            val process = createCompletingLoggingProcess(42)
            expectThat(process).get { pid() }.isGreaterThan(0)
        }

        @Test
        fun `should provide IO`() {
            val process = createLoopingLoggingProcess()
            expectThat(process).log.logs(OUT typed "test out", ERR typed "test err")
            process.destroyForcibly()
        }
    }

    @Nested
    inner class Interaction {

        @Slow @Test
        fun `should provide output processor access to own running process`() {
            val process: LoggingProcess = Processes.startShellScript {
                !"""
                 while true; do
                    >&1 echo "test out"
                    >&2 echo "test err"

                    read -p "Prompt: " READ
                    >&2 echo "${'$'}READ"
                    >&1 echo "${'$'}READ"
                done
                """.trimIndent()
            }.process { io ->
                if (io.type != META) {
                    kotlin.runCatching {
                        enter("just read $io")
                    }.recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                }
            }

            poll { process.ioLog.logged.size >= 6 }.every(100.milliseconds)
                .forAtMost(800.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
            process.destroy()

            expectThat(process) {
                destroyed
                    .isA<LoggingProcess>()
                    .log.get("logged %s") { logged.drop(2).take(4) }.containsExactlyInSomeOrder {
                        +(OUT typed "test out") + (ERR typed "test err")
                        +(IO.Type.IN typed "just read ${OUT.format("test out")}") + (IO.Type.IN typed "just read ${ERR.format("test err")}")
                    }
            }
        }
    }

    @Nested
    inner class Termination {

        @TestFactory
        fun `by waiting using`() = listOf(
            Assertion.Builder<LoggingProcess>::waitedFor,
            Assertion.Builder<LoggingProcess>::waitedForSomeDuration,
            Assertion.Builder<LoggingProcess>::waitedForSomeTimeUnits,
        ).test { waitOperation ->
            expect {
                measureTime {
                    that(createCompletingLoggingProcess()) {
                        log.logs { any { it.type != META } }
                        waitOperation.invoke(this).completesSuccessfully().not { isAlive() }
                    }
                }.also { that(it).isLessThanOrEqualTo(5.seconds) }
            }
        }

        @Test
        fun `by waiting not long enough`() {
            val process = createLoopingLoggingProcess()
            expectThat(process) {
                log.logs { any { it.type != META } }
                get { waitFor(1.milliseconds) }.isFalse()
            }
            process.destroyForcibly()
        }

        @Test
        fun `by waiting for success on failure`() {
            val process = createCompletingLoggingProcess(42)
            expectCatching { process.waitFor() }.isFailure().get { toString() }.containsDump()
        }

        @TestFactory
        fun `by destroying using`() = listOf(
            Assertion.Builder<LoggingProcess>::destroyed,
            Assertion.Builder<LoggingProcess>::destroyedForcibly,
        ).map { destroyOperation ->
            dynamicTest(destroyOperation.name) {
                expect {
                    measureTime {
                        that(createLoopingLoggingProcess()) {
                            log.logs { any { it.type != META } }
                            destroyOperation.invoke(this).waitedForSomeTimeUnits.not { isAlive() }
                        }
                    }.also { that(it).isLessThanOrEqualTo(5.seconds) }
                }
            }
        }

        @Test
        fun `should provide exit code`() {
            val process = createCompletingLoggingProcess(42)
            expectThat(process).waitedForSomeTimeUnits.exitValue.isEqualTo(42)
        }

        @Test
        fun `should not be alive`() {
            val process = createCompletingLoggingProcess(42)
            expectThat(process).waitedForSomeTimeUnits.not { isAlive() }
        }

        @Nested
        inner class OfSuccessfulProcess {

            @Test
            fun `should meta log on exit`() {
                val process = createCompletingLoggingProcess(0)
                expectThat(process).completesSuccessfully()
                    .isA<LoggedProcess>().and { logTransforming { all.takeLast(2) }.any { contains("terminated successfully") } }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                expect {
                    that(createCompletingLoggingProcess(exitValue = 0, processTerminationCallback = { callbackCalled = true }))
                        .completesSuccessfully()
                    expectThat(callbackCalled).isTrue()
                }
            }
        }

        @Nested
        inner class OfUnsuccessfulProcess {

            @Test
            fun `should meta log on exit`() {
                val process = createCompletingLoggingProcess(42)
                expect {
                    catching { process.waitFor() }.isFailure()
                    expectThat(process).mergedLog.contains("terminated with exit code 42.")
                }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                expect {
                    that(createCompletingLoggingProcess(exitValue = 42, processTerminationCallback = { callbackCalled = true }))
                        .waitedForSomeTimeUnits
                    200.milliseconds.sleep()
                    expectThat(callbackCalled).isTrue()
                }
            }

            @Test
            fun `should meta log dump`() {
                val process = createCompletingLoggingProcess(42)
                expect {
                    catching { process.waitFor() }.isFailure()
                    that(process).mergedLog.containsDump()
                }
            }
        }

        @Nested
        inner class OfFailedProcess {

            @Nested
            inner class ThrownException {

                @Test
                fun `should occur on exit`() {
                    expectCatching { createThrowingLoggingProcess().onExit().get() }.failed.and {
                        isA<ExecutionException>().cause.isA<ProcessExecutionException>()
                    }
                }

                @Test
                fun `should contain dump in message`() {
                    expectCatching { createThrowingLoggingProcess().onExit().get() }.failed.and {
                        get { toString() }.containsDump()
                    }
                }

                @Test
                fun `should have proper root cause`() {
                    expectCatching { createThrowingLoggingProcess().onExit().get() }.failed.and {
                        rootCause.isA<IllegalStateException>().message.isEqualTo("test")
                    }
                }
            }

            @Test
            fun `should meta log on exit`() {
                val process = createThrowingLoggingProcess()
                expect {
                    catching { process.onExit().get() }.failed
                    that(process).mergedLog.containsDump()
                }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                val process = createThrowingLoggingProcess(processTerminationCallback = { callbackCalled = true })
                expect {
                    catching { process.onExit().get() }.failed
                    that(callbackCalled).isTrue()
                }
            }
        }
    }
}

private fun IntervalPolling.forAShortTimeOrFail(message: String) = every(100.milliseconds).forAtMost(5.seconds) { fail(message) }
private fun IntervalPolling.ioOrFail() = forAShortTimeOrFail("Missing I/O Log")


fun <T : LoggedProcess, R> Assertion.Builder<T>.logTransforming(function: LoggedProcess.() -> R) = get(function)
val <T : LoggedProcess> Assertion.Builder<T>.plainLog get() = get("plain log") { toString() }
val <T : LoggingProcess> Assertion.Builder<T>.mergedLog
    get() = get("merged log") {
        ioLog.logged.joinToString("\n")
    }

fun Assertion.Builder<String>.containsDump() {
    compose("contains dump") {
        contains("dump has been written")
        contains(".sh")
        contains(".log")
        contains(".no-ansi.log")
    }.then { if (allPassed) pass() else fail() }
}

val <T : LoggingProcess> Assertion.Builder<T>.log get() = get("log %s") { ioLog }

fun <T : IOLog> Assertion.Builder<T>.logs(vararg io: IO) = logs(io.toList())
fun <T : IOLog> Assertion.Builder<T>.logs(io: Collection<IO>) = logsWithin(io = io)
fun <T : IOLog> Assertion.Builder<T>.logs(predicate: List<IO>.() -> Boolean) = logsWithin(predicate = predicate)

fun <T : IOLog> Assertion.Builder<T>.logsWithin(timeFrame: Duration = 5.seconds, vararg io: IO) = logsWithin(timeFrame, io.toList())
fun <T : IOLog> Assertion.Builder<T>.logsWithin(timeFrame: Duration = 5.seconds, io: Collection<IO>) =
    assert("logs $io within $timeFrame") { ioLog ->
        when (poll {
            ioLog.logged.containsAll(io)
        }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("logged ${ioLog.logged} instead")
        }
    }

fun <T : IOLog> Assertion.Builder<T>.logsWithin(timeFrame: Duration = 5.seconds, predicate: List<IO>.() -> Boolean) =
    assert("logs within $timeFrame") { ioLog ->
        when (poll {
            ioLog.logged.predicate()
        }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("did not log within $timeFrame")
        }
    }


private val tempDir: Path = tempDir().deleteOnExit()


fun createLoopingLoggingProcess() = Processes.executeShellScript(shellScript = createLoopingScript())
fun createThrowingLoggingProcess(
    processTerminationCallback: (() -> Unit)? = null,
) = Processes.executeShellScript(
    processTerminationCallback = processTerminationCallback,
    shellScript = createLoopingScript()) {
    check(it.type == META) {
        "test"
    }
}

fun createCompletingLoggingProcess(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
): LoggingProcess = Processes.executeShellScript(
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    shellScript = createCompletingScript(exitValue, sleep))
