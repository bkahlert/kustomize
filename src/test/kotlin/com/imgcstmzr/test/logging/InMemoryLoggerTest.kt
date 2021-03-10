package com.imgcstmzr.test.logging

import koodies.io.ByteArrayOutputStream
import koodies.logging.InMemoryLogger
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.startsWith

@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
@Isolated("flaky OutputCapture")
class InMemoryLoggerTest {

    @Test
    fun `should log using OutputStream`(capturedOutput: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger("caption", true, -1, listOf(outputStream))
        logger.logLine { "abc" }

        expectThat(capturedOutput).isEmpty()
        expectThat(outputStream).toStringContainsAll("caption", "abc")
    }

    @Test
    fun `should provide access to logs`(output: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger("caption", true, -1, listOf(outputStream))
        logger.logLine { "abc" }

        expectThat(logger.logged.removeEscapeSequences())
            .contains("caption")
            .contains("abc")
    }

    @Test
    fun `should use BlockRenderingLogger to logs`(output: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger("caption", true, -1, listOf(outputStream))

        expectThat(logger.logged.removeEscapeSequences()).startsWith("╭─────╴caption")
    }
}


fun <T : InMemoryLogger> T.logged(vararg texts: String): Assertion.Builder<String> =
    expectThatLogged().compose("contains text %s") { completeLog ->
        texts.forEach { text -> contains(text) }
    }.then { if (allPassed) pass() else fail() }

fun <T : InMemoryLogger> T.expectThatLogged() =
    expectThat(logged)

fun <T> Assertion.Builder<T>.toStringContainsAll(vararg expected: String): Assertion.Builder<T> =
    if (expected.size == 1) toStringContains(expected.single())
    else compose("contains %s", expected.joinToString(", ")) {
        expected.forEach { toStringContains(it) }
    }.then { if (allPassed && passedCount > 0) pass() else fail() }

infix fun <T> Assertion.Builder<T>.toStringContains(expected: String): Assertion.Builder<T> =
    assert("contains %s", expected) {
        when (val actual = it.toString().contains(expected)) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }
