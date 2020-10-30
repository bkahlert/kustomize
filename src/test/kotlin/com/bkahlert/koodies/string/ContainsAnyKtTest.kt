package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class ContainsAnyKtTest {
    val string = "foo bar"

    @Test
    internal fun `should return true if any of the others is case-matching substring`() {
        expectThat(string.containsAny(listOf("baz", "o b", "abc"))).isTrue()
    }

    @Test
    internal fun `should return true if any of the others is non-case-matching substring but case is ignored`() {
        expectThat(string.containsAny(listOf("baz", "O B", "abc"), ignoreCase = true)).isTrue()
    }

    @Test
    internal fun `should return false if none of the others is no case-matching substring`() {
        expectThat(string.containsAny(listOf("baz", "O B", "abc"))).isFalse()
    }

    @Test
    internal fun `should return false if none of the others is substring`() {
        expectThat(string.containsAny(listOf("baz", "---", "abc"))).isFalse()
    }
}
