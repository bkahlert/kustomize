package com.imgcstmzr.process

import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.bkahlert.koodies.test.strikt.matches
import com.imgcstmzr.process.Exec.Sync.execShellScript
import com.imgcstmzr.process.Output.Type.META
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
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.Collections

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
@Isolated // flaky OutputCapture
internal class ExecTest {

    @Nested
    inner class Check {

        @Nested
        inner class IfOutputContains {

            @Test
            internal fun `should assert present string`() {
                expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "Test", caseSensitive = false)).isTrue()
            }

            @Test
            internal fun `should assert missing string`() {
                expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "Missing", caseSensitive = false)).isFalse()
            }

            @Test
            internal fun `should assert present string case-sensitive`() {
                expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "test", caseSensitive = true)).isTrue()
            }

            @Test
            internal fun `should assert missing string case-sensitive`() {
                expectThat(Exec.Sync.checkIfOutputContains("echo 'this is a test'", "Test", caseSensitive = true)).isFalse()
            }
        }
    }

    @Test
    internal fun `should process all output types`(output: CapturedOutput) {
        val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
        val outputProcessor: (Output) -> Unit = { redirectedOutput.add(it) }

        execShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }

        expectThat(redirectedOutput.first { it.type == META }).matches("Executing {}")
        expectThat(redirectedOutput.first { it.type == Output.Type.OUT }).isEqualToStringWise("test output")
        expectThat(redirectedOutput.first { it.type == Output.Type.ERR }).isEqualToStringWise("test error")
        expectThat(redirectedOutput.map { it.removeEscapeSequences<CharSequence>() }).hasSize(3).contains("test output", "test error")
    }

    @Test
    internal fun `should process without logging to System out or in`(output: CapturedOutput) {
        val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
        val outputProcessor: (Output) -> Unit = { redirectedOutput.add(it) }

        execShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }

        expectThat(output).get { out }.isEmpty()
        expectThat(output).get { err }.isEmpty()
    }

    @Test
    internal fun `should format merged output`(output: CapturedOutput) {
        val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
        val outputProcessor: (Output) -> Unit = { redirectedOutput.add(it) }

        execShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }

        expectThat(redirectedOutput.first { it.type == META }).matches("Executing {}")
        expectThat(redirectedOutput.first { it.type == Output.Type.OUT }).isEqualToStringWise(Output.Type.OUT.format("test output"), removeAnsi = false)
        expectThat(redirectedOutput.first { it.type == Output.Type.ERR }).isEqualToStringWise(Output.Type.ERR.format("test error"), removeAnsi = false)
    }
}
