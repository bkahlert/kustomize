package com.imgcstmzr.test.debug

import com.imgcstmzr.test.logging.CapturedOutput
import com.imgcstmzr.test.logging.OutputCaptureExtension
import com.imgcstmzr.test.logging.expectThatLogged
import com.imgcstmzr.test.logging.toStringContains
import koodies.concurrent.process.IO
import koodies.logging.InMemoryLogger
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
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
    fun InMemoryLogger.`should not automatically log to console without @Debug`(output: CapturedOutput) {
        logStatus { IO.Type.OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThatLogged().contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.removeEscapeSequences()).not { toStringContains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun InMemoryLogger.`should not catch exceptions`(output: CapturedOutput) {
        logStatus { IO.Type.OUT typed "(*｀へ´*)" }

        expectCatching { logResult<Any> { Result.failure(IllegalStateException("test")) } }
            .isFailure().isA<IllegalStateException>()
    }
}