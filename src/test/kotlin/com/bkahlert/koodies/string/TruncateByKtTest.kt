package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
class TruncateByKtTest {
    @Test
    fun `should remove whitespaces from the right`() {
        expectThat("a   b   c".truncateBy(3)).isEqualTo("a  b c")
    }

    @Test
    fun `should use whitespaces on the right`() {
        expectThat("a   b   c    ".truncateBy(3)).isEqualTo("a   b   c ")
    }

    @Test
    fun `should use single whitespace on the right`() {
        expectThat("a   b   c ".truncateBy(1)).isEqualTo("a   b   c")
    }

    @Test
    fun `should not merge words`() {
        expectThat("a   b   c".truncateBy(10)).isEqualTo("a b c")
    }

    @Test
    fun `should consider all unicode whitespaces`() {
        val allWhitespaces = Unicode.whitespaces.joinToString("")
        expectThat("a ${allWhitespaces}b".truncateBy(allWhitespaces.length)).isEqualTo("a b")
    }

    @Test
    fun `should leave area before startIndex unchanged`() {
        expectThat("a   b   c".truncateBy(10, startIndex = 5)).isEqualTo("a   b c")
    }

    @Test
    fun `should leave whitespace sequence below minimal length unchanged`() {
        expectThat("a      b   c".truncateBy(3, minWhitespaceLength = 3)).isEqualTo("a   b   c")
    }

    @Test
    fun regression() {
        val x = "│   nested 1                                                                                            ▮▮"
        val y = "│   nested 1                                                                                      ▮▮"
        val z = "│   nested 1                                                                                         ▮▮"
        expectThat(x.truncateBy(3, minWhitespaceLength = 3)).isEqualTo(z).not { isEqualTo(y) }
    }
}
