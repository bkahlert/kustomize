package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class WithSuffixKtTest {
    @Test
    fun `should append suffix if missing`() {
        expectThat("foo".withSuffix("bar")).isEqualTo("foobar")
    }

    @Test
    fun `should fully append suffix if partially missing`() {
        expectThat("foob".withSuffix("bar")).isEqualTo("foobbar")
    }

    @Test
    fun `should not append suffix if present`() {
        expectThat("foobar".withSuffix("bar")).isEqualTo("foobar")
    }
}
