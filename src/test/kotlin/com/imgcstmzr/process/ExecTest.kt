package com.imgcstmzr.process

import com.bkahlert.koodies.test.strikt.matches
import com.imgcstmzr.process.Exec.Sync.execShellScript
import com.imgcstmzr.process.Exec.redirectMsg
import com.imgcstmzr.util.asString
import com.imgcstmzr.util.isEqualToStringWise
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.OutputCaptureExtension
import com.imgcstmzr.util.stripOffAnsi
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
import strikt.assertions.isEqualTo
import java.io.ByteArrayOutputStream
import java.util.Collections

@ExtendWith(OutputCaptureExtension::class)
@Execution(ExecutionMode.CONCURRENT)
internal class ExecTest {

    @Nested
    inner class WithoutMultiplexedLogging {

        @Nested
        @Isolated
        inner class IsolationWrapper {
            @Test
            internal fun `should log nothing by default`(output: CapturedOutput) {
                execShellScript { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }
                expectThat(output).get { all }.isEmpty()
                expectThat(output).get { out }.isEmpty()
                expectThat(output).get { err }.isEmpty()
            }

        }

        @Test
        internal fun `should log all output types separately`(output: CapturedOutput) {
            val msg = ByteArrayOutputStream()
            val out = ByteArrayOutputStream()
            val err = ByteArrayOutputStream()
            execShellScript(customizer = {
                redirectMsg(msg)
                redirectErrorStream(false)
                redirectOutput(out)
                redirectError(err)
            }) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }
            expectThat(msg).matches("""
                                Executing [sh, -c, >&1 echo "test output"
                                >&2 echo "test error"] in {}
                                Started Process[pid={}, exitValue={}]
                                Process[pid={}, exitValue={}] stopped with exit code {}
                                """.trimIndent())
            expectThat(out).asString().isEqualTo("test output")
            expectThat(err).asString().isEqualTo("test error")
        }
    }

    @Nested
    inner class WithMultiplexedLogging {

        @Test
        internal fun `should multiplex all output types`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
            val outputProcessor: Process?.(Output) -> Unit = { redirectedOutput.add(it) }

            execShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }

            expectThat(redirectedOutput.filter { it.type == Output.Type.META })
                .hasSize(4)
                .get { joinToString("\n") }
                .matches("""
                Executing [sh, -c, >&1 echo "test output"
                >&2 echo "test error"] in {}
                Started Process[pid={}, exitValue={}]
                Process[pid={}, exitValue={}] stopped with exit code {}
                """.trimIndent())
            expectThat(redirectedOutput.first { it.type == Output.Type.OUT }).isEqualToStringWise("test output")
            expectThat(redirectedOutput.first { it.type == Output.Type.ERR }).isEqualToStringWise("test error")
            expectThat(redirectedOutput.map { it.stripOffAnsi() }).hasSize(6).contains("test output", "test error")
        }

        @Test
        internal fun `should multiplex without logging to System out or in`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
            val outputProcessor: Process?.(Output) -> Unit = { redirectedOutput.add(it) }

            execShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }

            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        internal fun `should format multiplexed output`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<Output>())
            val outputProcessor: Process?.(Output) -> Unit = { redirectedOutput.add(it) }

            execShellScript(outputProcessor = outputProcessor) { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }

            expectThat(redirectedOutput.first { it.type == Output.Type.META }).isEqualToStringWise(Output.Type.META.format("Executing [sh, -c, >&1 echo \"test output\""),
                removeAnsi = false)
            expectThat(redirectedOutput.first { it.type == Output.Type.OUT }).isEqualToStringWise(Output.Type.OUT.format("test output"), removeAnsi = false)
            expectThat(redirectedOutput.first { it.type == Output.Type.ERR }).isEqualToStringWise(Output.Type.ERR.format("test error"), removeAnsi = false)
        }
    }
}
