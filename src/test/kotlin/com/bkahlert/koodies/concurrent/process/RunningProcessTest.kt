package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.regex.RegularExpressions
import com.bkahlert.koodies.regex.sequenceOfAllMatches
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
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
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message
import java.net.URL
import java.util.concurrent.CompletableFuture

@Execution(CONCURRENT)
class RunningProcessTest {

    private fun getRunningProcess(vararg io: IO) = object : RunningProcess() {
        override val process: Process = nullProcess
        override val ioLog: IOLog = IOLog().apply { io.forEach { add(it.type, it.string.toByteArray()) } }
        override val result: CompletableFuture<CompletedProcess>
            get() {
                return CompletableFuture.completedFuture(CompletedProcess(-1, -1, ioLog.logged))
            }
    }

    @Test
    fun `should format CompleteFuture simplified`() {
        val runningProcess = getRunningProcess()

        expectThat("$runningProcess").contains("; Completed normally;")
    }

    @Nested
    inner class OnExitCodeMismatch {

        @Test
        fun `should print last IO lines`() {
            expectCatching {
                getRunningProcess(
                    IO.Type.META typed "test 1$LF",
                    IO.Type.IN typed "test 2$LF",
                    IO.Type.OUT typed "test 3$LF",
                    IO.Type.ERR typed "test 4$LF",
                    IO.Type.META typed "test 5$LF",
                    IO.Type.IN typed "test 6$LF",
                    IO.Type.OUT typed "test 7$LF",
                    IO.Type.ERR typed "test 8$LF",
                    IO.Type.META typed "test 9$LF",
                    IO.Type.IN typed "test 10$LF",
                    IO.Type.OUT typed "test 11$LF",
                    IO.Type.ERR typed "test 12$LF",
                ).waitForExitCode(143)
            }
                .isFailure()
                .isA<IllegalStateException>()
                .message
                .isNotNull()
                .contains("last 10 lines")
                .get { lines().dropLast(1).takeLast(10).map { it.trim() } }
                .containsExactly(
                    IO.Type.OUT formatted "test 3",
                    IO.Type.ERR formatted "test 4",
                    IO.Type.META formatted "test 5",
                    IO.Type.IN formatted "test 6",
                    IO.Type.OUT formatted "test 7",
                    IO.Type.ERR formatted "test 8",
                    IO.Type.META formatted "test 9",
                    IO.Type.IN formatted "test 10",
                    IO.Type.OUT formatted "test 11",
                    IO.Type.ERR formatted "test 12",
                )
        }

        @Test
        fun `should save IO log`() {
            expectCatching {
                getRunningProcess(
                    IO.Type.OUT typed "test out$LF",
                    IO.Type.ERR typed "test err$LF",
                ).waitForExitCode(143)
            }
                .isFailure()
                .isA<IllegalStateException>()
                .message.get {
                    this?.let { message ->
                        RegularExpressions.urlRegex.sequenceOfAllMatches(message)
                            .map { URL(it) }
                            .map { it.openStream().reader().readText() }
                            .map { it.removeEscapeSequences() }
                            .toList()
                    } ?: fail("error message missing")
                }.hasSize(2).all {
                    contains("test out")
                    contains("test err")
                }
        }
    }
}
