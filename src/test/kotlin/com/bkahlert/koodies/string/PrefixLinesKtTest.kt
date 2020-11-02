package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
class PrefixLinesKtTest {
    @Test
    fun `should add prefix to each line`() {
        val prefixedLines = "12345     12345\nsnake    snake".prefixLinesWith(ignoreTrailingSeparator = true, "ab ")
        expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake")
    }

    @Test
    fun `should do nothing on empty prefix`() {
        val prefixedLines = "12345     12345\nsnake    snake".prefixLinesWith(ignoreTrailingSeparator = true, "")
        expectThat(prefixedLines).isEqualTo("12345     12345\nsnake    snake")
    }

    @Test
    fun `should keep trailing new line`() {
        val prefixedLines = "12345     12345\nsnake    snake\n".prefixLinesWith(ignoreTrailingSeparator = true, "ab ")
        expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake\n")
    }

    @Test
    fun `should prefix trailing new line if not ignored`() {
        val prefixedLines = "12345     12345\nsnake    snake\n".prefixLinesWith(ignoreTrailingSeparator = false, "ab ")
        expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake\nab ")
    }
}
