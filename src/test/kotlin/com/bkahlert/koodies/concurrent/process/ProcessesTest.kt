package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.Processes.cheapEvalShellScript
import com.bkahlert.koodies.concurrent.process.Processes.evalShellScript
import com.bkahlert.koodies.concurrent.process.Processes.executeShellScript
import com.bkahlert.koodies.concurrent.process.Processes.startShellScriptDetached
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.ShellScript
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
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isTrue
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

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
            val redirectedOutput = Collections.synchronizedList(mutableListOf<IO>())
            val processor: Processor<DelegatingProcess> = { redirectedOutput.add(it) }

            executeShellScript { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.process(processor = processor).waitFor()

            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should format merged output`(output: CapturedOutput) {
            val redirectedOutput = Collections.synchronizedList(mutableListOf<IO>())
            val processor: Processor<DelegatingProcess> = { redirectedOutput.add(it) }

            executeShellScript { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.process(processor = processor).waitFor()

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


fun createLoopingScript() = ShellScript {
    !"""
        while true; do
            >&1 echo "test out"
            >&2 echo "test err"
            sleep 1
        done
    """.trimIndent()
}

fun createCompletingScript(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
) = ShellScript {
    !"""
        >&1 echo "test out"
        >&2 echo "test err"
        ${sleep.takeIf { it.isPositive() }?.let { "sleep ${sleep.inSeconds}" } ?: ""}
        exit $exitValue
    """.trimIndent()
}


fun <T : Process> Assertion.Builder<T>.isAlive() = assert("is alive") {
    if (it.isAlive) pass() else fail("is not alive: ${(it as? LoggingProcess)?.ioLog?.dump() ?: "(${it::class.simpleName}—dump unavailable)"}")
}

val <T : Process> Assertion.Builder<T>.waitedFor
    get() = get("with waitFor() called") { also { waitFor() } }

val <T : DelegatingProcess> Assertion.Builder<T>.waitedForSomeDuration
    get() = get("with waitFor(1.seconds) called") { also { waitFor(1.seconds) } }

val <T : Process> Assertion.Builder<T>.waitedForSomeTimeUnits
    get() = get("with waitFor(1, TimeUnit.SECONDS) called") { also { waitFor(1, TimeUnit.SECONDS) } }

val <T : Process> Assertion.Builder<T>.destroyed
    get() = get("with destroy() called") { also { destroy() } }

val <T : Process> Assertion.Builder<T>.destroyedForcibly
    get() = get("with destroyForcibly() called") { also { destroyForcibly() } }

val <T : Process> Assertion.Builder<T>.completed
    get() = get("completed") {
        onExit().get()
    }

val <T : Process> Assertion.Builder<Result<T>>.failed
    get() = get("failed") { exceptionOrNull() }.isA<ExecutionException>()

fun <T : Process> Assertion.Builder<T>.completesSuccessfully(): Assertion.Builder<Process> =
    completed.assert("successfully") {
        val actual = it.exitValue()
        when (actual == 0) {
            true -> pass()
            else -> fail("completed with $actual")
        }
    }

fun <T : Process> Assertion.Builder<T>.completesUnsuccessfully(): Assertion.Builder<Process> =
    completed.assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitValue()
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

fun <T : Process> Assertion.Builder<T>.completesUnsuccessfully(expected: Int): Assertion.Builder<Process> =
    completed.assert("unsuccessfully with exit code $expected") {
        when (val actual = it.exitValue()) {
            expected -> pass()
            0 -> fail("completed successfully")
            else -> fail("completed unsuccessfully with exit code $actual")
        }
    }
