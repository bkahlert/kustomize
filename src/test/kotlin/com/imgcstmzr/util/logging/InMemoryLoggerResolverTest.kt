package com.imgcstmzr.util.logging


import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.util.asString
import com.imgcstmzr.util.debug.Debug
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(ExecutionMode.CONCURRENT)
internal class InMemoryLoggerResolverTest {

    @Isolated // flaky OutputCapture
    @Nested
    inner class DebugTests {

        @Nested
        inner class SuccessTests {

            @Debug(includeInReport = false)
            @Test
            internal fun `should log to console automatically with @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
                logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
                logger.logLast(Result.success(Unit))

                expectThat(logger.logged).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
                expectThat(output.removeEscapeSequences()).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
            }
        }

        @Nested
        inner class FailureTests {

            @Debug(includeInReport = false)
            @Test
            internal fun `should log to console automatically with @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
                logger.logLine(OUT typed "(*｀へ´*)")

                expectCatching { logger.logLast(Result.failure(IllegalStateException("test"))) }
                    .isFailure().isA<IllegalStateException>()

                expectThat(logger.logged).asString().contains("(*｀へ´*)")
                expectThat(output.removeEscapeSequences()).asString().contains("(*｀へ´*)")
            }
        }
    }

    @Nested
    inner class NoDebugTests {
        @Nested
        inner class SuccessTests {

            @Test
            internal fun `should not automatically log to console without @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
                logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
                logger.logLast(Result.success(Unit))

                expectThat(logger.logged).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
                expectThat(output.removeEscapeSequences()).asString().not { contains("☎Σ⊂⊂(☉ω☉∩)") }
            }
        }

        @Nested
        inner class FailureTests {

            @Test
            internal fun `should not catch exceptions`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
                logger.logLine(OUT typed "(*｀へ´*)")

                expectCatching { logger.logLast(Result.failure(IllegalStateException("test"))) }
                    .isFailure().isA<IllegalStateException>()
            }
        }
    }
}