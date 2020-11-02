package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
class TruncateToKtTest {
    @Test
    fun `should remove whitespaces from the right`() {
        expectThat("a   b   c".truncateTo(6)).isEqualTo("a  b c")
    }

    @Test
    fun `should use whitespaces on the right`() {
        expectThat("a   b   c    ".truncateTo(10)).isEqualTo("a   b   c ")
    }

    @Test
    fun `should use single whitespace on the right`() {
        expectThat("a   b   c ".truncateTo(9)).isEqualTo("a   b   c")
    }

    @Test
    fun `should not merge words`() {
        expectThat("a   b   c".truncateTo(0)).isEqualTo("a b c")
    }

    @Test
    fun `should consider all unicode whitespaces`() {
        val allWhitespaces = Unicode.whitespaces.joinToString("")
        expectThat("a ${allWhitespaces}b".truncateTo(0)).isEqualTo("a b")
    }

    @Test
    fun `should leave area before startIndex unchanged`() {
        expectThat("a   b   c".truncateTo(0, startIndex = 5)).isEqualTo("a   b c")
    }

    @Test
    fun `should leave whitespace sequence below minimal length unchanged`() {
        expectThat("a      b   c".truncateTo(9, minWhitespaceLength = 3)).isEqualTo("a   b   c")
    }
}
