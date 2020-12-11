package com.imgcstmzr.util.logging

import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.test.junit.debug.Debug
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure

@Isolated("flaky OutputCapture")
@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class InMemoryLoggerResolverDebugTest {

    @Nested
    inner class SuccessTests {

        @Debug(includeInReport = false)
        @Test
        fun InMemoryLogger.`should log to console automatically with @Debug`(output: CapturedOutput) {
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }

            getExpectThatLogged().contains("☎Σ⊂⊂(☉ω☉∩)")
            expectThat(output.removeEscapeSequences()).contains("☎Σ⊂⊂(☉ω☉∩)")
        }
    }

    @Nested
    inner class FailureTests {

        @Debug(includeInReport = false)
        @Test
        fun InMemoryLogger.`should log to console automatically with @Debug`(output: CapturedOutput) {
            logStatus { OUT typed "(*｀へ´*)" }

            expectCatching { logResult { Result.failure<Any>(IllegalStateException("test")) } }
                .isFailure().isA<IllegalStateException>()

            getExpectThatLogged().contains("(*｀へ´*)")
            expectThat(output.removeEscapeSequences()).contains("(*｀へ´*)")
        }
    }
}
