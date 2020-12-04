@file:Suppress("FINAL_UPPER_BOUND", "unused", "BlockingMethodInNonBlockingContext")

package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.string.anyContainsAll
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.junit.test
import com.bkahlert.koodies.test.strikt.containsExactlyInSomeOrder
import com.bkahlert.koodies.time.IntervalPolling
import com.bkahlert.koodies.time.poll
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
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.message
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class LoggingProcessTest {

    @Nested
    inner class Startup {

        @Test
        fun `should throw on start failure`() {
            expectCatching { LoggingProcess(CommandLine("invalid something", Kaomojis.FallDown.random().toString())).waitForSuccess() }
                .isFailure().isA<ExecutionException>()
        }

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
            val process = Processes.startShellScript(processor = { output ->
                if (output.type != META) {
                    kotlin.runCatching {
                        enter("just read $output")
                    }.recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                }
            }) {
                !"""
                 while true; do
                    >&1 echo "test out"
                    >&2 echo "test err"

                    read -p "Prompt: " READ
                    >&2 echo "${'$'}READ"
                    >&1 echo "${'$'}READ"
                done
                """.trimIndent()
            }
            poll { process.ioLog.logged.size >= 6 }.every(100.milliseconds)
                .forAtMost(8.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
            process.destroy()

            expectThat(process) {
                destroyed
                completed.logTransforming { all.drop(2).take(4) }.containsExactlyInSomeOrder {
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
            Assertion.Builder<LoggingProcess>::waitedForSuccess,
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
            expectCatching { process.waitForSuccess() }.isFailure().get { toString() }.containsDump()
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
                            destroyOperation.invoke(this).completesUnsuccessfully().not { isAlive() }
                        }
                    }.also { that(it).isLessThanOrEqualTo(5.seconds) }
                }
            }
        }

        @Test
        fun `should provide exit code`() {
            val process = createCompletingLoggingProcess(42)
            expectThat(process).completesUnsuccessfully(42)
        }

        @Test
        fun `should not be alive`() {
            val process = createCompletingLoggingProcess(42)
            expectThat(process).completed.not { isAlive() }
        }

        @Nested
        inner class OfSuccessfulProcess {

            @Test
            fun `should meta log on exit`() {
                val process = createCompletingLoggingProcess(0)
                expectThat(process).completesSuccessfully().and { logTransforming { all.takeLast(2) }.any { contains("terminated successfully") } }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                expect {
                    that(createCompletingLoggingProcess(exitValue = 0, runAfterProcessTermination = { callbackCalled = true })).completesSuccessfully()
                    expectThat(callbackCalled).isTrue()
                }
            }
        }

        @Nested
        inner class OfUnsuccessfulProcess {

            @Test
            fun `should meta log on exit`() {
                val process = createCompletingLoggingProcess(42)
                expectThat(process).completesUnsuccessfully().and { plainLog.contains("terminated with exit code 42.") }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                expect {
                    that(createCompletingLoggingProcess(exitValue = 42, runAfterProcessTermination = { callbackCalled = true })).completesUnsuccessfully()
                    expectThat(callbackCalled).isTrue()
                }
            }

            @Test
            fun `should meta log dump`() {
                val process = createCompletingLoggingProcess(42)
                expectThat(process).completesUnsuccessfully().and {
                    plainLog.containsDump()
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
                        message.isNotNull()
                            .contains("RuntimeException: Process[pid=")
                            .contains("IllegalStateException: test")
                        rootCause.isA<IllegalStateException>().message.isEqualTo("test")
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
                    that(process) {
                        log.logs {
                            anyContainsAll(listOf("RuntimeException: Process[pid=", "IllegalStateException: test"))
                        }
                    }
                }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                val process = createThrowingLoggingProcess(runAfterProcessTermination = { callbackCalled = true })
                expect {
                    catching { process.onExit().get() }.failed
                    that(callbackCalled).isTrue()
                }
            }

            @Test
            fun `should meta log dump`() {
                val process = createThrowingLoggingProcess()
                expect {
                    catching { process.onExit().get() }.failed
                    that(process) { mergedLog.containsDump() }
                }
            }
        }
    }
}

private fun IntervalPolling.forAShortTimeOrFail(message: String) = every(100.milliseconds).forAtMost(5.seconds) { fail(message) }
private fun IntervalPolling.ioOrFail() = forAShortTimeOrFail("Missing I/O Log")


fun <T : Process> Assertion.Builder<T>.isAlive() = assert("is alive") {
    if (it.isAlive) pass() else fail("is not alive: ${(it as? LoggingProcess)?.ioLog?.dump() ?: "(${it::class.simpleName}—dump unavailable)"}")
}

val <T : LoggingProcess> Assertion.Builder<T>.waitedForSuccess
    get() = get("with waitForSuccess() called") { also { waitForSuccess() } }

val <T : Process> Assertion.Builder<T>.waitedFor
    get() = get("with waitFor() called") { also { waitFor() } }

val <T : LoggingProcess> Assertion.Builder<T>.waitedForSomeDuration
    get() = get("with waitFor(1.seconds) called") { also { waitFor(1.seconds) } }

val <T : Process> Assertion.Builder<T>.waitedForSomeTimeUnits
    get() = get("with waitFor(1, TimeUnit.SECONDS) called") { also { waitFor(1, TimeUnit.SECONDS) } }

val <T : Process> Assertion.Builder<T>.destroyed
    get() = get("with destroy() called") { also { destroy() } }

val <T : Process> Assertion.Builder<T>.destroyedForcibly
    get() = get("with destroyForcibly() called") { also { destroyForcibly() } }

val <T : Process> Assertion.Builder<T>.completed
    get() = get("completed") { onExit().get() }

val <T : LoggingProcess> Assertion.Builder<T>.completed
    get() = get("completed") { onExit().get() }.isA<LoggedProcess>()

val <T : Process> Assertion.Builder<Result<T>>.failed
    get() = get("failed") { exceptionOrNull() }.isA<ExecutionException>()

fun <T : LoggingProcess> Assertion.Builder<T>.completesSuccessfully(): Assertion.Builder<LoggedProcess> =
    completed.assert("successfully") {
        val actual = it.exitValue()
        when (actual == 0) {
            true -> pass()
            else -> fail("completed with $actual")
        }
    }

fun <T : LoggingProcess> Assertion.Builder<T>.completesUnsuccessfully(): Assertion.Builder<LoggedProcess> =
    completed.assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitValue()
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

fun <T : LoggingProcess> Assertion.Builder<T>.completesUnsuccessfully(expected: Int): Assertion.Builder<LoggedProcess> =
    completed.assert("unsuccessfully with exit code $expected") {
        when (val actual = it.exitValue()) {
            expected -> pass()
            0 -> fail("completed successfully")
            else -> fail("completed unsuccessfully with exit code $actual")
        }
    }

fun <T : LoggedProcess, R> Assertion.Builder<T>.logTransforming(function: LoggedProcess.() -> R) = get(function)
val <T : LoggedProcess> Assertion.Builder<T>.plainLog get() = get("plain log") { toString() }
val <T : LoggingProcess> Assertion.Builder<T>.mergedLog get() = get("plain log") { ioLog.logged.joinToString("\n") }
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
        when (poll { ioLog.logged.containsAll(io) }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("logged ${ioLog.logged} instead")
        }
    }

fun <T : IOLog> Assertion.Builder<T>.logsWithin(timeFrame: Duration = 5.seconds, predicate: List<IO>.() -> Boolean) =
    assert("logs within $timeFrame") { ioLog ->
        when (poll { ioLog.logged.predicate() }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("did not log within $timeFrame")
        }
    }


fun createLoopingLoggingProcess(
    runAfterProcessTermination: () -> Unit = {},
    processor: Processor? = { },
) = Processes.startShellScript(runAfterProcessTermination = runAfterProcessTermination, processor = processor) {
    !"""
        while true; do
            >&1 echo "test out"
            >&2 echo "test err"
            sleep 1
        done
    """.trimIndent()
}

fun createThrowingLoggingProcess(
    runAfterProcessTermination: () -> Unit = {},
) = createLoopingLoggingProcess(runAfterProcessTermination = runAfterProcessTermination) {
    check(it.type == META) {
        "test"
    }
}

fun createCompletingLoggingProcess(
    exitValue: Int = 0,
    runAfterProcessTermination: () -> Unit = {},
    sleep: Duration = Duration.ZERO,
): LoggingProcess = Processes.startShellScript(runAfterProcessTermination = runAfterProcessTermination) {
    !"""
        >&1 echo "test out"
        >&2 echo "test err"
        ${sleep.takeIf { it.isPositive() }?.let { "sleep ${sleep.inSeconds}" }}
        exit $exitValue
    """.trimIndent()
}
