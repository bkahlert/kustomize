package com.imgcstmzr.util.logging

import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.test.strikt.toStringContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class InMemoryLoggerResolverNoDebugTest {

    @Test
    fun `should not automatically log to console without @Debug`(output: CapturedOutput, logger: InMemoryLogger) {
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }

        expectThat(logger.logged).contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.removeEscapeSequences()).not { toStringContains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun `should not catch exceptions`(output: CapturedOutput, logger: InMemoryLogger) {
        logger.logStatus { OUT typed "(*｀へ´*)" }

        expectCatching { logger.logResult<Any> { Result.failure(IllegalStateException("test")) } }
            .isFailure().isA<IllegalStateException>()
    }
}
