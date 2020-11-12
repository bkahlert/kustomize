package com.imgcstmzr.util.logging

import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.imgcstmzr.util.asString
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
    fun `should not automatically log to console without @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }

        expectThat(logger.logged).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.removeEscapeSequences()).asString().not { contains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun `should not catch exceptions`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
        logger.logStatus { OUT typed "(*｀へ´*)" }

        expectCatching { logger.logResult { Result.failure(IllegalStateException("test")) } }
            .isFailure().isA<IllegalStateException>()
    }
}
