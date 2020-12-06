package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.synchronized
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
import strikt.assertions.cause
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
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
class DelegatingProcessTest {

    @Nested
    inner class Creation {
        @Test
        fun `should not start on its own`() {
            expectCatching {
                createThrowingDelegatingProcess()
                200.milliseconds.sleep()
            }.isSuccess()
        }

        @Test
        fun `should not start if delegated`() {
            expectCatching {
                DelegatingProcess(createThrowingDelegatingProcess())
                200.milliseconds.sleep()
            }.isSuccess()
        }

        @Test
        fun `should not start on direct toString`() {
            val process = createThrowingDelegatingProcess()
            expectThat(process.toString())
                .isEqualTo("DelegatingProcess[delegate=not yet initialized; commandLine=command; expectedExitValue=0; processTerminationCallback=\uD83D\uDFE2; destroyOnShutdown=✅]")
        }

        @Test
        fun `should not start on indirect toString`() {
            val process = createThrowingDelegatingProcess()
            val delegatingProcess = DelegatingProcess(process)
            expectThat(delegatingProcess.toString())
                .isEqualTo("DelegatingProcess[delegate=not yet initialized; commandLine=command; expectedExitValue=0; processTerminationCallback=\uD83D\uDFE2; destroyOnShutdown=✅]")
        }
    }


    @Nested
    inner class Startup {

        @Test
        fun `should start if accessed directly`() {
            val process = createCompletingDelegatingProcess()
            expectThat(process).completed.exitValue.isEqualTo(0)
        }

        @Test
        fun `should start if accessed indirectly`() {
            val delegatingProcess = DelegatingProcess(createCompletingDelegatingProcess())
            expectThat(delegatingProcess).completed.exitValue.isEqualTo(0)
        }

        @Test
        fun `should provide string on direct toString`() {
            val process = createCompletingDelegatingProcess()
            expectThat(process.toString())
                .matchesCurlyPattern("DelegatingProcess[delegate=Process[pid={}, exitValue={}]; result={}; commandLine={}; expectedExitValue=0; processTerminationCallback={}; destroyOnShutdown={}]")
        }

        @Test
        fun `should provide string on indirect toString`() {
            val process = createCompletingDelegatingProcess()
            val delegatingProcess = DelegatingProcess(process)
            expectThat(delegatingProcess.toString())
                .matchesCurlyPattern("DelegatingProcess[delegate=Process[pid={}, exitValue={}]; result={}; commandLine={}; expectedExitValue=0; processTerminationCallback={}; destroyOnShutdown={}]")
        }

        @Test
        fun `should be alive`() {
            val process = createCompletingDelegatingProcess(sleep = 5.seconds)
            expectThat(process).isAlive()
            process.destroyForcibly()
        }

        @Test
        fun `should provide PID`() {
            val process = createCompletingDelegatingProcess(42)
            expectThat(process).get { pid() }.isGreaterThan(0)
        }
    }

    @Nested
    inner class Interaction {

        @Slow @Test
        fun `should provide output processor access to own running process`() {
            val logged = mutableListOf<Pair<String, IO.Type>>().synchronized()
            val delegatingProcess = Processes.startShellScript {
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

            delegatingProcess.process { io ->
                logged.add(io.unformatted to io.type)
                if (io.type != IO.Type.META) {
                    kotlin.runCatching {
                        enter("just read $io")
                    }.recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                }
            }

            poll { logged.size >= 8 }.every(100.milliseconds)
                .forAtMost(8.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
            delegatingProcess.destroy()

            expect {
                that(delegatingProcess) {
                    destroyed
                    exitValue.not { isEqualTo(0) }
                }
                that(logged.take(4) + logged.drop(6).take(2)).containsExactlyInSomeOrder {
                    +("test out" to IO.Type.OUT) + ("test err" to IO.Type.ERR)
                    +("just read test out" to IO.Type.OUT) + ("just read test out" to IO.Type.ERR) + ("just read test err" to IO.Type.OUT) + ("just read test err" to IO.Type.ERR)
                }
            }
        }
    }

    @Nested
    inner class Termination {

        @TestFactory
        fun `by waiting using`() = listOf(
            Assertion.Builder<DelegatingProcess>::waitedFor,
            Assertion.Builder<DelegatingProcess>::waitedForSomeDuration,
            Assertion.Builder<DelegatingProcess>::waitedForSomeTimeUnits,
        ).test { waitOperation ->
            expect {
                measureTime {
                    that(createCompletingDelegatingProcess()) {
                        waitOperation.invoke(this).completesSuccessfully().not { isAlive() }
                    }
                }.also { that(it).isLessThanOrEqualTo(5.seconds) }
            }
        }

        @Test
        fun `by waiting not long enough`() {
            val process = createLoopingDelegatingProcess()
            expect {
                catching { process.exitValue() }.isFailure().isA<IllegalThreadStateException>()
                that(process).get { waitFor(1.milliseconds) }.isFalse()
            }
            process.destroyForcibly()
        }

        @TestFactory
        fun `by destroying using`() = listOf(
            Assertion.Builder<DelegatingProcess>::destroyed,
            Assertion.Builder<DelegatingProcess>::destroyedForcibly,
        ).map { destroyOperation ->
            DynamicTest.dynamicTest(destroyOperation.name) {
                expect {
                    measureTime {
                        that(createLoopingDelegatingProcess()) {
                            destroyOperation.invoke(this).waitedForSomeTimeUnits.not { isAlive() }
                            exitValue.not { isEqualTo(0) }
                        }
                    }.also { that(it).isLessThanOrEqualTo(5.seconds) }
                }
            }
        }

        @Test
        fun `should provide exit code`() {
            val process = createCompletingDelegatingProcess(exitValue = 42, expectedExitValue = 42)
            expectThat(process).completed.exitValue.isEqualTo(42)
        }

        @Test
        fun `should not be alive`() {
            val process = createCompletingDelegatingProcess(0)
            expectThat(process).completed.not { isAlive() }
        }

        @Nested
        inner class OfSuccessfulProcess {

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                expect {
                    that(createCompletingDelegatingProcess(exitValue = 0, processTerminationCallback = {
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
            fun `should throw on waitFor`() {
                val process = createCompletingDelegatingProcess(42)
                expectCatching { process.waitFor() }.isFailure()
                    .isA<CompletionException>()
                    .cause.isA<ProcessExecutionException>()
                    .message.isNotNull()
                    .lines().first().matchesCurlyPattern("Process {} terminated with exit code 42. Expected 0.")
            }


            @Test
            fun `should throw on exit`() {
                val process = createCompletingDelegatingProcess(42)
                expectCatching { process.onExit().get() }.isFailure()
                    .isA<ExecutionException>()
                    .rootCauseMessage
                    .isNotNull()
                    .lines().first().matchesCurlyPattern("Process {} terminated with exit code 42. Expected 0.")
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                val process = createCompletingDelegatingProcess(42, processTerminationCallback = { callbackCalled = true })
                expect {
                    catching { process.onExit().get() }.isFailure()
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
                    expectCatching { createThrowingDelegatingProcess().onExit().get() }.failed.and {
                        message.isNotNull()
                        rootCause.isA<IllegalStateException>().message.isEqualTo("process triggered")
                    }
                }
            }

            @Test
            fun `should call callback`() {
                var callbackCalled = false
                val process = createThrowingDelegatingProcess(processTerminationCallback = { callbackCalled = true })
                expect {
                    catching { process.onExit().get() }.failed
                    that(callbackCalled).isTrue()
                }
            }
        }
    }

}

fun createLoopingDelegatingProcess(): DelegatingProcess = Processes.executeShellScript(shellScript = createLoopingScript())
fun createThrowingDelegatingProcess(
    processTerminationCallback: (() -> Unit)? = {
        throw ExecutionException(IllegalStateException("callback called"))
    },
): DelegatingProcess =
    DelegatingProcess(lazy {
        throw ExecutionException(IllegalStateException("process triggered"))
    }, CommandLine("command"), 0, processTerminationCallback, true)

fun createCompletingDelegatingProcess(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
): DelegatingProcess = Processes.startShellScript(
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    shellScript = createCompletingScript(exitValue, sleep)).also {
    it.info() // call something to trigger execution
}
