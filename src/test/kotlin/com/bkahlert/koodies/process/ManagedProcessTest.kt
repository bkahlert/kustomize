package com.bkahlert.koodies.process

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.ProcessExecutionException
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.process.containsDump
import com.bkahlert.koodies.concurrent.process.createCompletingScript
import com.bkahlert.koodies.concurrent.process.createLoopingScript
import com.bkahlert.koodies.concurrent.process.logs
import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.exception.rootCauseMessage
import com.bkahlert.koodies.string.lines
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.junit.test
import com.bkahlert.koodies.test.strikt.containsExactlyInSomeOrder
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.time.poll
import com.bkahlert.koodies.time.sleep
import org.junit.jupiter.api.DynamicTest
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
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class ManagedProcessTest {

    @Nested
    inner class Creation {
        @Test
        fun `should not start on its own`() {
            expectCatching {
                createThrowingManagedProcess()
                200.milliseconds.sleep()
            }.isSuccess()
        }
    }


    @Nested
    inner class Startup {

        @Test
        fun `should start if accessed directly`() {
            val process = createCompletingManagedProcess()
            expectThat(process).completed.exitValue.isEqualTo(0)
        }

        @Test
        fun `should provide string on direct toString`() {
            val process = createCompletingManagedProcess()
            expectThat(process.toString())
                .matchesCurlyPattern("ManagedProcess[delegate=Process[pid={}, exitValue={}]; result={}; commandLine={}; expectedExitValue=0; processTerminationCallback={}; destroyOnShutdown={}]")
        }

        @Test
        fun `should be alive`() {
            val process = createCompletingManagedProcess(sleep = 5.seconds)
            expectThat(process).alive
            process.kill()
        }

        @Test
        fun `should meta log documents`() {
            val process = createCompletingManagedProcess()
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
            val process = createCompletingManagedProcess(42)
            expectThat(process).get { pid }.isGreaterThan(0)
        }

        @Test
        fun `should provide IO`() {
            val process = createLoopingManagedProcess()
            expectThat(process).log.logs(IO.Type.OUT typed "test out", IO.Type.ERR typed "test err")
            process.kill()
        }
    }

    @Nested
    inner class Interaction {

        @Slow @Test
        fun `should provide output processor access to own running process`() {
            val process: ManagedProcess = startShellScript {
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
                if (io.type != IO.Type.META) {
                    kotlin.runCatching {
                        enter("just read $io")
                    }.recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                }
            }

            poll { process.ioLog.logged.size >= 6 }.every(100.milliseconds)
                .forAtMost(800.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
            process.stop()

            println(process.ioLog.logged)

            expectThat(process) {
                killed
                    .log.get("logged %s") { logged.drop(2).take(4) }.containsExactlyInSomeOrder {
                        +(IO.Type.OUT typed "test out") + (IO.Type.ERR typed "test err")
                        +(IO.Type.IN typed "just read ${IO.Type.OUT.format("test out")}") + (IO.Type.IN typed "just read ${IO.Type.ERR.format("test err")}")
                    }
            }
        }
    }

    @Nested
    inner class Termination {

        @TestFactory
        fun `by waiting using`() = listOf(
            Assertion.Builder<ManagedProcess>::waitedFor,
        ).test { waitOperation ->
            expect {
                measureTime {
                    that(createCompletingManagedProcess()) {
                        waitOperation.invoke(this).completesSuccessfully().not { alive }
                    }
                }.also { that(it).isLessThanOrEqualTo(5.seconds) }
            }
        }

        @TestFactory
        fun `by destroying using`() = listOf(
            Assertion.Builder<ManagedProcess>::stopped,
            Assertion.Builder<ManagedProcess>::killed,
        ).map { destroyOperation ->
            DynamicTest.dynamicTest(destroyOperation.name) {
                expect {
                    measureTime {
                        that(createLoopingManagedProcess()) {
                            catching { destroyOperation.invoke(this).waitedFor }.isFailure()
                            not { alive }
                            exitValue.not { isEqualTo(0) }
                        }
                    }.also { that(it).isLessThanOrEqualTo(5.seconds) }
                }
            }
        }

        @Test
        fun `should provide exit code`() {
            val process = createCompletingManagedProcess(exitValue = 42, expectedExitValue = 42)
            expectThat(process).completed.exitValue.isEqualTo(42)
        }

        @Test
        fun `should not be alive`() {
            val process = createCompletingManagedProcess(0)
            expectThat(process).completed.not { alive }
        }

        @Nested
        inner class OfSuccessfulProcess {

            @Test
            fun `should meta log on exit`() {
                val process = createCompletingManagedProcess(0)
                expectThat(process).completesSuccessfully()
                    .isA<ManagedProcess>().and { get { ioLog.logged.takeLast(2) } any { contains("terminated successfully") } }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                expect {
                    that(createCompletingManagedProcess(exitValue = 0, processTerminationCallback = {
                        callbackCalled = true
                    })).completesSuccessfully()
                    100.milliseconds.sleep()
                    expectThat(callbackCalled).isTrue()
                }
            }
        }

        @Nested
        inner class OnExitCodeMismatch {

            @Test
            fun `should meta log on exit`() {
                val process = createCompletingManagedProcess(42)
                expect {
                    catching { process.waitFor() }.isFailure()
                    expectThat(process).mergedLog.contains("terminated with exit code 42.")
                }
            }

            @Test
            fun `should meta log dump`() {
                val process = createCompletingManagedProcess(42)
                expect {
                    catching { process.waitFor() }.isFailure()
                    that(process).mergedLog.containsDump()
                }
            }

            @Test
            fun `should throw on waitFor`() {
                val process = createCompletingManagedProcess(42)
                expectCatching { process.waitFor() }.isFailure()
                    .isA<CompletionException>()
                    .cause.isA<ProcessExecutionException>()
                    .message.isNotNull()
                    .lines().first().matchesCurlyPattern("Process {} terminated with exit code 42. Expected 0.")
            }


            @Test
            fun `should throw on exit`() {
                val process = createCompletingManagedProcess(42)
                expectCatching { process.onExit.get() }.isFailure()
                    .isA<ExecutionException>()
                    .rootCauseMessage
                    .isNotNull()
                    .lines().first().matchesCurlyPattern("Process {} terminated with exit code 42. Expected 0.")
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                val process = createCompletingManagedProcess(42, processTerminationCallback = { callbackCalled = true })
                expect {
                    catching { process.onExit.get() }.isFailure()
                    expectThat(callbackCalled).isTrue()
                }
            }
        }

        @Nested
        inner class OfFailedProcess {

            @Nested
            inner class ThrownException {

                @Test
                fun `should occur on exit`() {
                    expectCatching { createThrowingManagedProcess().onExit.get() }.failed.and {
                        message.isNotNull()

                    }
                }

                @Test
                fun `should contain dump in message`() {
                    expectCatching { createThrowingManagedProcess().onExit.get() }.failed.and {
                        get { toString() }.containsDump()
                    }
                }

                @Test
                fun `should have proper root cause`() {
                    expectCatching { createThrowingManagedProcess().onExit.get() }.failed.and {
                        rootCause.isA<IllegalStateException>().message.isEqualTo("test")
                    }
                }
            }

            @Test
            fun `should meta log on exit`() {
                val process = createThrowingManagedProcess()
                expect {
                    catching { process.onExit.get() }.failed
                    that(process).mergedLog.containsDump()
                }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                val process = createThrowingManagedProcess(processTerminationCallback = { callbackCalled = true })
                expect {
                    catching { process.onExit.get() }.failed
                    that(callbackCalled).isTrue()
                }
            }
        }
    }

}

fun createLoopingManagedProcess(): ManagedProcess = executeShellScript(shellScript = createLoopingScript())
fun createThrowingManagedProcess(
    processTerminationCallback: (() -> Unit)? = null,
) = executeShellScript(
    processTerminationCallback = processTerminationCallback,
    shellScript = createLoopingScript()) {
    check(it.type == IO.Type.META) {
        "test"
    }
}

fun createCompletingManagedProcess(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = startShellScript(
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    shellScript = createCompletingScript(exitValue, sleep)).also {
    it.alive // call something to trigger execution
}


val <T : ManagedProcess> Assertion.Builder<T>.alive: Assertion.Builder<T>
    get() = assert("is alive") { if (it.alive) pass() else fail("is not alive: ${it.ioLog.dump()}") }

val <T : ManagedProcess> Assertion.Builder<T>.log get() = get("log %s") { ioLog }

val <T : ManagedProcess> Assertion.Builder<T>.mergedLog
    get() = get("merged log") {
        ioLog.logged.joinToString("\n")
    }

val <T : ProcessV> Assertion.Builder<T>.exitValue: Assertion.Builder<Int>
    get() = get("with exit value %s") { exitValue }

val <T : ProcessV> Assertion.Builder<T>.waitedFor: Assertion.Builder<T>
    get() = get("with waitFor() called") { also { waitFor() } }

val <T : ProcessV> Assertion.Builder<T>.stopped: Assertion.Builder<T>
    get() = get("with stop() called") { stop() as T }

val <T : ProcessV> Assertion.Builder<T>.killed: Assertion.Builder<T>
    get() = get("with kill() called") { kill() as T }

val <T : ProcessV> Assertion.Builder<T>.completed: Assertion.Builder<T>
    get() = get("completed") { onExit.get() as T }

val <T : ProcessV> Assertion.Builder<Result<T>>.failed: Assertion.Builder<ExecutionException>
    get() = get("failed") { exceptionOrNull() }.isA()

fun <T : ProcessV> Assertion.Builder<T>.completesSuccessfully(): Assertion.Builder<T> =
    completed.assert("successfully") {
        val actual = it.exitValue
        when (actual == 0) {
            true -> pass()
            else -> fail("completed with $actual")
        }
    }

fun <T : ProcessV> Assertion.Builder<T>.completesUnsuccessfully(): Assertion.Builder<T> =
    completed.assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitValue
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

fun <T : ProcessV> Assertion.Builder<T>.completesUnsuccessfully(expected: Int): Assertion.Builder<T> =
    completed.assert("unsuccessfully with exit code $expected") {
        when (val actual = it.exitValue) {
            expected -> pass()
            0 -> fail("completed successfully")
            else -> fail("completed unsuccessfully with exit code $actual")
        }
    }
