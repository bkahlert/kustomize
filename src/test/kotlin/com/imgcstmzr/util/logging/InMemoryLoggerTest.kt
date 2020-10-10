package com.imgcstmzr.util.logging

import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.imgcstmzr.util.asString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.startsWith
import java.io.ByteArrayOutputStream

@Execution(ExecutionMode.CONCURRENT)
@Isolated // flaky OutputCapture
internal class InMemoryLoggerTest {
    @Test
    internal fun `should log using OutputStream`(capturedOutput: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger<Unit>("caption", true, listOf(outputStream))
        logger.log("abc", true)

        expectThat(capturedOutput).isEmpty()
        expectThat(outputStream).asString()
            .contains("caption")
            .contains("abc")
    }

    @Test
    internal fun `should provide access to logs`(output: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger<Unit>("caption", true, listOf(outputStream))
        logger.log("abc", true)

        expectThat(logger.logged.removeEscapeSequences())
            .contains("caption")
            .contains("abc")
    }

    @Test
    internal fun `should use BlockRenderingLogger to logs`(output: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger<Unit>("caption", true, listOf(outputStream))

        expectThat(logger.logged.removeEscapeSequences()).startsWith("╭─────╴caption")
    }
}
