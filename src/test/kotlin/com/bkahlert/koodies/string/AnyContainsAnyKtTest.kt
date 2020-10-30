package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
internal class AnyContainsAnyKtTest {

    val stringList = listOf("foo bar", "BAR BAZ")

    @Test
    internal fun `should return true if any of the others is case-matching substring`() {
        expectThat(stringList.anyContainsAny(listOf("baz", "o b", "abc"))).isTrue()
    }

    @Test
    internal fun `should return true if any of the others is non-case-matching substring but case is ignored`() {
        expectThat(stringList.anyContainsAny(listOf("Baz", "---", "abc"), ignoreCase = true)).isTrue()
    }

    @Test
    internal fun `should return false if none of the others is no case-matching substring`() {
        expectThat(stringList.anyContainsAny(listOf("baz", "Baz", "abc"))).isFalse()
    }

    @Test
    internal fun `should return false if none of the others is substring`() {
        expectThat(stringList.anyContainsAny(listOf("@@@", "---", "!!!"))).isFalse()
    }
}
