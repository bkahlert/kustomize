package com.imgcstmzr.util.debug


import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.util.asString
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.OutputCaptureExtension
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import org.junit.platform.commons.support.AnnotationSupport
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
@TestMethodOrder(OrderAnnotation::class)
internal class DebugTest {

    @Test
    internal fun `should run in isolation`(output: CapturedOutput) {
        expectThat(AnnotationSupport.findAnnotation(Debug::class.java, Isolated::class.java).orElse(null))
            .isNotNull()
    }

    @Nested
    inner class DebugTests {

        var siblingTestRun = false

        @Order(1)
        @Test
        internal fun `should not run due to sibling @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
            var siblingTestRun = false
            fail { "This test should have been disabled due to @Debug on sibling test." }
        }

        @Order(2)
        @Debug(includeInReport = false)
        @Test
        internal fun `should run (and check if non-@Debug) did not run`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
            expectThat(siblingTestRun).isFalse()
        }
    }

    @Nested
    inner class NoDebugTests {

        @Test
        internal fun `should not automatically log to console without @Debug`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
            logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logger.logResult { Result.success(Unit) }

            expectThat(logger.logged).asString().contains("☎Σ⊂⊂(☉ω☉∩)")
            expectThat(output.removeEscapeSequences()).asString().not { contains("☎Σ⊂⊂(☉ω☉∩)") }
        }


        @Test
        internal fun `should not catch exceptions`(output: CapturedOutput, logger: InMemoryLogger<Unit>) {
            logger.logStatus { OUT typed "(*｀へ´*)" }

            expectCatching { logger.logResult { Result.failure(IllegalStateException("test")) } }
                .isFailure().isA<IllegalStateException>()
        }
    }
}
