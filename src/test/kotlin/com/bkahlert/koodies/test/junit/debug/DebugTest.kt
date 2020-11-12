package com.bkahlert.koodies.test.junit.debug

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.imgcstmzr.util.asString
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.OutputCaptureExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import org.junit.platform.commons.support.AnnotationSupport
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNotNull

@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class DebugTest {

    @Test
    fun `should run in isolation`(output: CapturedOutput) {
        expectThat(AnnotationSupport.findAnnotation(Debug::class.java, Isolated::class.java).orElse(null)).isNotNull()
    }

    @Test
    fun `should not automatically log to console without @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
        logger.logStatus { IO.Type.OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logResult { Result.success(Unit) }

        expectThat(logger.logged).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.removeEscapeSequences()).asString().not { contains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun `should not catch exceptions`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
        logger.logStatus { IO.Type.OUT typed "(*｀へ´*)" }

        expectCatching { logger.logResult { Result.failure(IllegalStateException("test")) } }
            .isFailure().isA<IllegalStateException>()
    }
}
