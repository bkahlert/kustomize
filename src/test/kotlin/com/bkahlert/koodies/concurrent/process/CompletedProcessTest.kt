package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.nio.file.conditioned
import com.bkahlert.koodies.regex.RegularExpressions
import com.bkahlert.koodies.regex.sequenceOfAllMatches
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isEqualToStringWise
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.filter
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message
import strikt.assertions.single
import java.io.IOException
import java.net.URL

@Execution(CONCURRENT)
class CompletedProcessTest {

    @Nested
    inner class SaveIO {

        @Test
        fun `should save IO to specified file`() {
            val completedProcess = createCompletedTestProcess()
            val logPaths = completedProcess.saveIO(path = Paths.tempFile(CompletedProcess::class.simpleName!!, ".txt").apply { delete() }.deleteOnExit())
            expectThat(logPaths.map { it.deleteOnExit().readAll().removeEscapeSequences() }).hasSize(2).all {
                isEqualTo("""
                Starting process...
                processing
                awaiting input: 
                cancel
                invalid input
                an abnormal error has occurred (errno 99)
            """.trimIndent())
            }.get { CompletedProcess::class.simpleName!! }
        }

        @Test
        fun `should  throw if IO could not be saved`() {
            val completedProcess = createCompletedTestProcess()
            val logPath = Paths.tempFile(CompletedProcess::class.simpleName!!, ".log").writeText("already exists")
            logPath.toFile().setReadOnly()
            expectCatching {
                completedProcess.saveIO(path = logPath.deleteOnExit())
            }.isFailure().isA<IOException>()
            logPath.toFile().setWritable(true)
        }

        @Test
        fun `should save IO to file with ansi formatting`() {
            val completedProcess = createCompletedTestProcess()
            val logPaths = completedProcess.saveIO().onEach { it.deleteOnExit() }
            expectThat(logPaths).filter { it.conditioned.endsWith("ansi.log") }.single().hasContent("""
                ${IO.Type.META.format("Starting process...")}
                ${IO.Type.OUT.format("processing")}
                ${IO.Type.OUT.format("awaiting input: ")}
                ${IO.Type.IN.format("cancel")}
                ${IO.Type.ERR.format("invalid input")}
                ${IO.Type.ERR.format("an abnormal error has occurred (errno 99)")}
            """.trimIndent())
        }

        @Test
        fun `should save IO to file without ansi formatting`() {
            val completedProcess = createCompletedTestProcess()
            val logPaths = completedProcess.saveIO().onEach { it.deleteOnExit() }
            expectThat(logPaths).filter { !it.conditioned.endsWith("ansi.log") }.single().hasContent("""
                Starting process...
                processing
                awaiting input: 
                cancel
                invalid input
                an abnormal error has occurred (errno 99)
            """.trimIndent())
        }
    }

    @Nested
    inner class CheckExitCode {
        @Test
        fun `should do nothing on matching existCode`() {
            val completedProcess = createCompletedTestProcess()
            val returnValue = completedProcess.checkExitCode(99) { fail("failed") }
            expectThat(returnValue).isEqualTo(completedProcess)
        }

        @Test
        fun `should log result lines on mismatch`() {
            val completedProcess = createCompletedTestProcess()
            expectCatching { completedProcess.checkExitCode() }
                .isFailure()
                .isA<IllegalStateException>()
                .message
                .isNotNull()
                .contains("99")
                .contains("0")
                .contains("expected")
                .contains("error")
        }

        @Test
        fun `should save log on mismatch`() {
            val completedProcess = createCompletedTestProcess()
            expectCatching { completedProcess.checkExitCode() }
                .isFailure()
                .isA<IllegalStateException>()
                .message.get {
                    this?.let {
                        RegularExpressions.urlRegex.sequenceOfAllMatches(it)
                            .map { URL(it) }
                            .map { it.openStream().reader().readText() }
                            .map { it.removeEscapeSequences() }
                            .toList()
                    } ?: fail("error message missing")
                }.hasSize(2).all {
                    isEqualTo("""
                    Starting process...
                    processing
                    awaiting input: 
                    cancel
                    invalid input
                    an abnormal error has occurred (errno 99)
                """.trimIndent())
                }
        }

        @Test
        fun `should log last lines on mismatch`() {
            val completedProcess = createCompletedTestProcess()
            expectCatching { completedProcess.checkExitCode() }
                .isFailure()
                .isA<IllegalStateException>()
                .message
                .isNotNull()
                .contains(IO.Type.META.format("Starting process..."))
                .contains(IO.Type.OUT.format("processing"))
                .contains(IO.Type.OUT.format("awaiting input: "))
                .contains(IO.Type.IN.format("cancel"))
                .contains(IO.Type.ERR.format("invalid input"))
                .contains(IO.Type.ERR.format("an abnormal error has occurred (errno 99)"))
        }

        @Test
        fun `should log all lines if problem saving the log`() {
            val completedProcess = createCompletedTestProcess()
            val logPaths = Paths.tempFile(CompletedProcess::class.simpleName!!, ".txt").writeText("already exists")
            logPaths.toFile().setReadOnly()
            expectCatching { completedProcess.checkExitCode() }
                .isFailure()
                .isA<IllegalStateException>()
                .message
                .isNotNull()
                .contains(IO.Type.META.format("Starting process..."))
                .contains(IO.Type.OUT.format("processing"))
                .contains(IO.Type.OUT.format("awaiting input: "))
                .contains(IO.Type.IN.format("cancel"))
                .contains(IO.Type.ERR.format("invalid input"))
                .contains(IO.Type.ERR.format("an abnormal error has occurred (errno 99)"))
            logPaths.toFile().setWritable(true)
        }

        @Test
        fun `should run custom callback on mismatch`() {
            val completedProcess = createCompletedTestProcess()
            val path = Paths.tempFile(CompletedProcess::class.simpleName!!, ".txt").writeText("already exists")
            path.toFile().setReadOnly()
            expectCatching { completedProcess.checkExitCode() { "custom message" } }
                .isFailure()
                .isA<IllegalStateException>()
                .message
                .isNotNull()
                .isEqualTo("custom message")
            path.toFile().setWritable(true)
        }
    }

    @Test
    fun `should provide complete IO log`() {
        val completedProcess = createCompletedTestProcess()
        val (pid, exitCode, all, meta, input, output, error) = completedProcess

        expectThat(pid).isEqualTo(42L)
        expectThat(exitCode).isEqualTo(99)
        expectThat(completedProcess).isEqualToStringWise("""
                ${IO.Type.META.format("Starting process...")}
                ${IO.Type.OUT.format("processing")}
                ${IO.Type.OUT.format("awaiting input: ")}
                ${IO.Type.IN.format("cancel")}
                ${IO.Type.ERR.format("invalid input")}
                ${IO.Type.ERR.format("an abnormal error has occurred (errno 99)")}
            """.trimIndent())
        expectThat(all).containsExactly(
            IO.Type.META typed "Starting process...",
            IO.Type.OUT typed "processing",
            IO.Type.OUT typed "awaiting input: ",
            IO.Type.IN typed "cancel",
            IO.Type.ERR typed "invalid input",
            IO.Type.ERR typed "an abnormal error has occurred (errno 99)",
        )
        expectThat(meta.lines()).containsExactly(
            IO.Type.META typed "Starting process...",
        )
        expectThat(input.lines()).containsExactly(
            IO.Type.IN typed "cancel",
        )
        expectThat(output.lines()).containsExactly(
            IO.Type.OUT typed "processing",
            IO.Type.OUT typed "awaiting input: ",
        )
        expectThat(error.lines()).containsExactly(
            IO.Type.ERR typed "invalid input",
            IO.Type.ERR typed "an abnormal error has occurred (errno 99)",
        )
    }
}

fun createCompletedTestProcess(): CompletedProcess {
    return CompletedProcess(42L, 99, listOf(
        IO.Type.META typed "Starting process...",
        IO.Type.OUT typed "processing",
        IO.Type.OUT typed "awaiting input: ",
        IO.Type.IN typed "cancel",
        IO.Type.ERR typed "invalid input",
        IO.Type.ERR typed "an abnormal error has occurred (errno 99)",
    ))
}
