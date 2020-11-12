package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.Processes.cheapEvalShellScript
import com.bkahlert.koodies.concurrent.process.Processes.evalShellScript
import com.bkahlert.koodies.concurrent.process.Processes.startShellScript
import com.bkahlert.koodies.concurrent.process.Processes.startShellScriptDetached
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.startAsDaemon
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.strikt.containsExactlyInSomeOrder
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.time.poll
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.containsAtLeast
import com.imgcstmzr.util.isEqualToStringWise
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.OutputCaptureExtension
import com.imgcstmzr.util.readAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.message
import java.util.Collections
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class ProcessesTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    @ExtendWith(OutputCaptureExtension::class)
    @Isolated // flaky OutputCapture
    inner class IfOutputContains {

        @Test
        fun `should assert present string`() {
            expectThat(Processes.checkIfOutputContains("echo 'this is a test'", "Test", caseSensitive = false)).isTrue()
        }

        @Test
        fun `should assert missing string`() {
            expectThat(Processes.checkIfOutputContains("echo 'this is a test'", "Missing", caseSensitive = false)).isFalse()
        }

        @Test
        fun `should assert present string case-sensitive`() {
            expectThat(Processes.checkIfOutputContains("echo 'this is a test'", "test", caseSensitive = true)).isTrue()
        }

        @Test
        fun `should assert missing string case-sensitive`() {
            expectThat(Processes.checkIfOutputContains("echo 'this is a test'", "Test", caseSensitive = true)).isFalse()
        }
    }

    @Nested
    @ExtendWith(OutputCaptureExtension::class)
    @Isolated // flaky OutputCapture
    inner class SynchronousExecution {

        @Test
        fun `should process without logging to System out or in`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<IO>())
            val ioProcessor: RunningProcess.(IO) -> Unit = { redirectedOutput.add(it) }

            startShellScript(ioProcessor = ioProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.waitForCompletion()

            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should format merged output`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<IO>())
            val ioProcessor: RunningProcess.(IO) -> Unit = { redirectedOutput.add(it) }

            startShellScript(ioProcessor = ioProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.waitForCompletion()

            expectThat(redirectedOutput.first { it.type == META }).matchesCurlyPattern("Executing {}")
            expectThat(redirectedOutput.first { it.type == OUT }).isEqualToStringWise(OUT.format("test output"), removeAnsi = false)
            expectThat(redirectedOutput.first { it.type == ERR }).isEqualToStringWise(ERR.format("test error"), removeAnsi = false)
        }
    }

    @Slow @Test
    fun `should run detached scripts`() {
        val path = tempDir.tempFile()
        val process = startShellScriptDetached {
            !"""sleep 3"""
            !"""echo $$ > "$path""""
        }
        var content = ""
        var waitCount = 0
        var aliveCount = 0
        val pid = process.pid()
        poll {
            waitCount++
            if (process.isAlive) aliveCount++
            path.readAll().also { content = it.trim() }.isNotBlank()
        }.every(500.milliseconds).forAtMost(8.seconds) {
            fail("No content was written to $path within 8 seconds.")
        }
        expectThat(waitCount).isGreaterThanOrEqualTo(4)
        expectThat(aliveCount).isGreaterThanOrEqualTo(4)
        expectThat(process.isAlive).isFalse()
        expectThat(content).isEqualTo("$pid")
    }

    @Test
    fun `should cheap eval script`() {
        val output = cheapEvalShellScript {
            !"""
                >&1 echo "test output"
                >&2 echo "test error"
            """.trimMargin()
        }
        expectThat(output).isEqualTo("test output")
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
        expectThat(completedProcess.output.lines()).containsExactly(OUT typed "test output")
        expectThat(completedProcess.error.lines()).containsExactly(ERR typed "test error")
    }

    @Test
    fun `should be destroyable`() {
        measureTime {
            val process = startShellScript {
                !"""
                while true; do
                    >&1 echo "test out"
                    >&2 echo "test err"
                    sleep 1
                done
                """.trimIndent()
            }
            poll { process.ioLog.logged.any { it.type != META } }.every(500.milliseconds).forAtMost(5.seconds) {
                fail("No I/O logged within 5 seconds.")
            }
            process.destroy()
            expectThat(process.waitForCompletion()) {
                get { output }.contains("test out")
                get { error }.contains("test err")
            }
        }.let { expectThat(it).isLessThanOrEqualTo(5.seconds) }
    }

    @Nested
    inner class OnExitCodeMismatch {
        @Test
        fun `should print script location`() {
            val process = startShellScript { }
            expectCatching { process.waitForExitCode(143) }
                .isFailure()
                .isA<IllegalStateException>()
                .message
                .isNotNull()
                .get { lines() }.any {
                    contains("📄 file:///")
                    contains(".sh")
                }
        }

        @Test
        fun `should dump IO`() {
            val process = startShellScript { !"echo \"test\"" }
            expectCatching { process.waitForExitCode(143) }
                .isFailure()
                .isA<IllegalStateException>()
                .message
                .isNotNull()
                .contains("dump has been written")
        }
    }

    @Test
    fun `should run after process termination on normal termination`() {
        measureTime {
            var ranAfterProcessTermination = false
            val process = startShellScript(
                ioProcessor = { },
                runAfterProcessTermination = { ranAfterProcessTermination = true }
            ) {
                !"""
                        >&1 echo "test out"
                        >&2 echo "test err"
                    """.trimIndent()
            }
            process.waitForCompletion()
            expectThat(ranAfterProcessTermination).isTrue()
        }.let { expectThat(it).isLessThanOrEqualTo(2.5.seconds) }
    }

    @Slow @Test
    fun `should run after process termination on destruction`() {
        measureTime {
            var ranAfterProcessTermination = false
            val process = startShellScript(
                ioProcessor = { },
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
            process.waitForCompletion()
            expectThat(ranAfterProcessTermination).isTrue()
        }.let { expectThat(it).isLessThanOrEqualTo(3.seconds) }
    }

    @Slow @Test
    fun `should process input`() {
        val process = startShellScript {
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

        poll {
            with(process.ioLog.logged) {
                any { it.type == OUT && it.unformatted == "test out" } && any { it.type == ERR && it.unformatted == "test err" }
            }
        }.every(100.milliseconds).forAtMost(5.seconds) { fail("Did not log I/O \"test out\"/\"test err\"") }

        process.enter("test in 0")
        poll { process.ioLog.logged.lastOrNull { it.unformatted.isNotBlank() }?.unformatted == "test in 0" }
            .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not log I/O \"test in 0\"") }

        process.enter("test in 1")
        poll { process.ioLog.logged.lastOrNull { it.unformatted.isNotBlank() }?.unformatted == "test in 1" }
            .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not log I/O \"test in 1\"") }

        process.destroy()
    }

    @Slow @Test
    fun `should provide output processor access to own running process`() {
        val process = startShellScript(ioProcessor = { output ->
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

        val (_, _, io, _, _, _, _) = process.waitForCompletion()
        expectThat(io.drop(2).take(4)).containsExactlyInSomeOrder {
            +(OUT typed "test out") + (ERR typed "test err")
            +(IN typed "just read ${OUT.format("test out")}") + (IN typed "just read ${ERR.format("test err")}")
        }
    }

    @Test
    fun `should provide PID`() {
        val process = startShellScript {
            !"""
                 exit 23
                """.trimIndent()
        }

        expectThat(process.waitForCompletion().pid).isGreaterThan(0)
    }

    @Test
    fun `should provide exit code`() {
        val process = startShellScript {
            !"""
                 exit 23
                """.trimIndent()
        }

        expectThat(process.waitForCompletion().exitCode).isEqualTo(23)
    }

    @Slow @Test
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
        }.also {
            it.enter("test in")
        }

        val (_, _, io, _, _, _, _) = process.waitForCompletion()

        expectThat(io.drop(2)).containsExactlyInSomeOrder {
            +(OUT typed "test out") + (ERR typed "test err") + (IN typed "test in")
            +(OUT typed "test in") + (ERR typed "test in") + (OUT typed "") + (ERR typed "")
        }
        expectThat(process.waitForCompletion().meta.removeEscapeSequences())
            .contains("Executing")
            .containsAtLeast("koodies.process", 2)
            .containsAtLeast(".sh", 2)
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
