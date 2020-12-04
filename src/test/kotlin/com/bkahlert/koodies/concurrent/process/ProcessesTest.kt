package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.Processes.cheapEvalShellScript
import com.bkahlert.koodies.concurrent.process.Processes.evalShellScript
import com.bkahlert.koodies.concurrent.process.Processes.startShellScript
import com.bkahlert.koodies.concurrent.process.Processes.startShellScriptDetached
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.strikt.isEqualToStringWise
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.time.poll
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.OutputCaptureExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isTrue
import java.util.Collections
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class ProcessesTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    @ExtendWith(OutputCaptureExtension::class)
    @Isolated("flaky OutputCapture")
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
    @Isolated("flaky OutputCapture")
    inner class SynchronousExecution {

        @Test
        fun `should process without logging to System out or in`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<IO>())
            val processor: Processor = { redirectedOutput.add(it) }

            startShellScript(processor = processor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.waitFor()

            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should format merged output`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<IO>())
            val ioProcessor: Processor = { redirectedOutput.add(it) }

            startShellScript(processor = ioProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.waitFor()

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
            path.readText().also { content = it.trim() }.isNotBlank()
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

        expectThat(completedProcess.exitValue()).isEqualTo(0)
        expectThat(completedProcess.output.lines()).containsExactly(OUT typed "test output")
        expectThat(completedProcess.error.lines()).containsExactly(ERR typed "test error")
    }
}
