package com.imgcstmzr.process

import com.bkahlert.koodies.concurrent.startAsDaemon
import com.bkahlert.koodies.concurrent.synchronized
import com.bkahlert.koodies.test.strikt.containsExactlyInSomeOrder
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.process.Exec.Async.startShellScript
import com.imgcstmzr.process.Exec.Sync.evalShellScript
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.IN
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.util.isEqualToStringWise
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.OutputCaptureExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isTrue
import java.util.Collections
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.seconds

@Execution(ExecutionMode.CONCURRENT)
class ExecTest {

    @Nested
    @ExtendWith(OutputCaptureExtension::class)
    @Isolated // flaky OutputCapture
    inner class IfOutputContains {

        @Test
        fun `should assert present string`() {
            expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "Test", caseSensitive = false)).isTrue()
        }

        @Test
        fun `should assert missing string`() {
            expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "Missing", caseSensitive = false)).isFalse()
        }

        @Test
        fun `should assert present string case-sensitive`() {
            expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "test", caseSensitive = true)).isTrue()
        }

        @Test
        fun `should assert missing string case-sensitive`() {
            expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "Test", caseSensitive = true)).isFalse()
        }
    }

    @Nested
    @ExtendWith(OutputCaptureExtension::class)
    @Isolated // flaky OutputCapture
    inner class SynchronousExecution {

        @Test
        fun `should process without logging to System out or in`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
            val outputProcessor: RunningProcess.(Output) -> Unit = { redirectedOutput.add(it) }

            startShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.waitForCompletion()

            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should format merged output`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
            val outputProcessor: RunningProcess.(Output) -> Unit = { redirectedOutput.add(it) }

            startShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.waitForCompletion()

            expectThat(redirectedOutput.first { it.type == META }).matchesCurlyPattern("Executing {}")
            expectThat(redirectedOutput.first { it.type == OUT }).isEqualToStringWise(OUT.format("test output"), removeAnsi = false)
            expectThat(redirectedOutput.first { it.type == ERR }).isEqualToStringWise(ERR.format("test error"), removeAnsi = false)
        }
    }

    @Test
    fun `should eval script`() {
        val completedProcess = evalShellScript {
            !"""
                >&1 echo "test output"
                >&2 echo "test error"
            """.trimMargin()
        }

        expectThat(completedProcess.exitCode).isEqualTo(0)
        expectThat(completedProcess.all).containsExactlyInAnyOrder(OUT typed "test output", ERR typed "test error")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should be destroyable`() {
        measureTime {
            val output = mutableSetOf<String>().synchronized()
            val process = startShellScript(outputProcessor = {
                if (it.type != META) output.add(it.unformatted)
            }) {
                !"""
                while true; do
                    >&1 echo "test out"
                    >&2 echo "test err"
                    sleep 1
                done
                """.trimIndent()
            }
            startAsDaemon(2.seconds) {
                process.destroy()
            }
            process.blockingExitCode
            expectThat(output).containsExactlyInAnyOrder("test out", "test err")
        }.let { expectThat(it).isLessThanOrEqualTo(2.5.seconds) }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should run after process termination on normal termination`() {
        measureTime {
            var ranAfterProcessTermination = false
            val process = startShellScript(
                outputProcessor = { },
                runAfterProcessTermination = { ranAfterProcessTermination = true }
            ) {
                !"""
                        >&1 echo "test out"
                        >&2 echo "test err"
                    """.trimIndent()
            }
            process.blockingExitCode
            expectThat(ranAfterProcessTermination).isTrue()
        }.let { expectThat(it).isLessThanOrEqualTo(2.5.seconds) }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should run after process termination on destruction`() {
        measureTime {
            var ranAfterProcessTermination = false
            val process = startShellScript(
                outputProcessor = { },
                runAfterProcessTermination = { ranAfterProcessTermination = true }
            ) {
                !"""
                        while true; do
                            >&1 echo "test out"
                            >&2 echo "test err"
                            sleep 1
                        done
                    """.trimIndent()
            }
            startAsDaemon(2.seconds) {
                process.destroy()
            }
            process.blockingExitCode
            expectThat(ranAfterProcessTermination).isTrue()
        }.let { expectThat(it).isLessThanOrEqualTo(2.5.seconds) }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should process input`() {
        val output = mutableSetOf<String>().synchronized()
        val process = startShellScript(outputProcessor = {
            if (it.type != META) output.add(it.unformatted)
        }) {
            !"""
                 while true; do
                    >&1 echo "test out"
                    >&2 echo "test err"

                    read -p "Prompt: " READ
                    >&2 echo "${'$'}READ"
                    >&1 echo "${'$'}READ"

                    sleep 1
                done
                """.trimIndent()
        }

        (0 until 2).map { i ->
            process.enter("test in $i")
            1.seconds.sleep()
        }
        1.seconds.sleep()
        process.destroy()
        process.waitFor()
        expectThat(output.also { it.remove("") }).containsExactlyInAnyOrder(
            "test out",
            "test err",
            "test in 0",
            "test in 1",
        )
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should provide output processor access to own running process`() {
        val process = startShellScript(outputProcessor = { output ->
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
       
                    sleep 1
                done
                """.trimIndent()
        }

        5.5.seconds.sleep()
        process.destroy()

        val (exitCode, io) = process.waitForCompletion()
        expectThat(exitCode).isEqualTo(143)
        expectThat(io).containsExactlyInSomeOrder {
            +(OUT typed "test out") + (ERR typed "test err")
            +(IN typed "just read ${OUT.format("test out")}") + (IN typed "just read ${ERR.format("test err")}")
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should return exit code`() {
        val process = startShellScript {
            !"""
                 exit 23
                """.trimIndent()
        }

        val (exitCode, _) = process.waitForCompletion()
        expectThat(exitCode).isEqualTo(23)
    }


    @OptIn(ExperimentalTime::class)
    @Test
    fun `should provide IO`() {
        val process = startShellScript {
            !"""
                    >&1 echo "test out"
                    >&2 echo "test err"

                    read -p "Prompt: " READ
                    >&2 echo "${'$'}READ"
                    >&1 echo "${'$'}READ"
       
                    sleep 1
                """.trimIndent()
        }.also { it.enter("test in") }

        val (_, io) = process.waitForCompletion()

        expectThat(io).containsExactlyInSomeOrder {
            +(IN typed "test in")
            +(OUT typed "test out") + (ERR typed "test err")
            +(OUT typed "test in") + (ERR typed "test in") + (OUT typed "") + (ERR typed "")
        }
        expectThat(process.waitForCompletion().input.lines()).containsExactly(
            IN typed "test in",
        )
        expectThat(process.waitForCompletion().output.lines()).containsExactly(
            OUT typed "test out",
            OUT typed "test in",
            OUT typed "",
        )
        expectThat(process.waitForCompletion().error.lines()).containsExactly(
            ERR typed "test err",
            ERR typed "test in",
            ERR typed "",
        )

        expectThat(process.waitForCompletion().all).containsExactly(io.subList(0, io.size))
        expectThat(process.waitForCompletion().input).isEqualTo("""
                test in
            """.trimIndent().let { IN typed it })
        expectThat(process.waitForCompletion().output).isEqualTo("""
                test out
                test in
                
            """.trimIndent().let { OUT typed it })
        expectThat(process.waitForCompletion().error).isEqualTo("""
                test err
                test in
                
            """.trimIndent().let { ERR typed it })
    }
}
