package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class PrefixLinesKtTest {
    @Test
    internal fun `should add prefix to each line`() {
        expectThat("12345     12345\nsnake    snake".prefixLinesWith("ab ")).isEqualTo("ab 12345     12345\nab snake    snake")
    }

    @Test
    internal fun `should do nothing on empty prefix`() {
        expectThat("12345     12345\nsnake    snake".prefixLinesWith("")).isEqualTo("12345     12345\nsnake    snake")
    }

    @Test
    internal fun `should keep trailing new line`() {
        expectThat("12345     12345\nsnake    snake\n".prefixLinesWith("ab ")).isEqualTo("ab 12345     12345\nab snake    snake\n")
    }
}
