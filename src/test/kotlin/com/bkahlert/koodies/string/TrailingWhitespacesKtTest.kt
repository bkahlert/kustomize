package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class TrailingWhitespacesKtTest {

    @Test
    internal fun `should find single whitespace`() {
        expectThat("abc ".trailingWhitespaces).isEqualTo(" ")
    }

    @Test
    internal fun `should find untypical whitespaces`() {
        expectThat(Unicode.whitespaces.joinToString("").trailingWhitespaces).hasLength(1)
    }

    @Test
    internal fun `should only last whitespace`() {
        expectThat("abc  ".trailingWhitespaces).isEqualTo(" ")
    }


    @Test
    internal fun `should not find non trailing whitespaces`() {
        expectThat("abc  x".trailingWhitespaces).isEqualTo("")
    }
}
