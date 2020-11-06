package com.imgcstmzr.util.logging


import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.test.junit.debug.Debug
import com.imgcstmzr.util.asString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure

@Isolated // flaky OutputCapture
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class InMemoryLoggerResolverDebugTest {

    @Nested
    inner class SuccessTests {

        @Debug(includeInReport = false)
        @Test
        fun `should log to console automatically with @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
            logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }

            expectThat(logger.logged).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
            expectThat(output.removeEscapeSequences()).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
        }
    }

    @Nested
    inner class FailureTests {

        @Debug(includeInReport = false)
        @Test
        fun `should log to console automatically with @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
            logger.logStatus { OUT typed "(*｀へ´*)" }

            expectCatching { logger.logResult { Result.failure(IllegalStateException("test")) } }
                .isFailure().isA<IllegalStateException>()

            expectThat(logger.logged).asString().contains("(*｀へ´*)")
            expectThat(output.removeEscapeSequences()).asString().contains("(*｀へ´*)")
        }
    }
}
