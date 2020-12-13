package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.Processes.evalScriptToOutput
import com.bkahlert.koodies.concurrent.process.Processes.evalShellScript
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.OutputCaptureExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(SAME_THREAD)
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
            executeShellScript() { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.silentlyProcess().waitFor()
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should format merged output`(output: CapturedOutput) {
            val process = executeShellScript() { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.silentlyProcess().also { it.waitFor() }
            expectThat(process.ioLog.logged) {
                get { first().type }.isEqualTo(META)
                get { first().unformatted }.matchesCurlyPattern("Executing {}")
                contains(OUT typed "test output", ERR typed "test error")
            }
        }
    }

    @Test
    fun `should cheap eval script`() {
        val output = evalScriptToOutput {
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
            """.trimMargin()
        }

        expectThat(completedProcess.output.lines()).containsExactly("test output")
        expectThat(completedProcess.exitValue).isEqualTo(0)
    }
}
