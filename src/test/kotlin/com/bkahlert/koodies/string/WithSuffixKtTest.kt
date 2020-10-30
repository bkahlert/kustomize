package com.bkahlert.koodies.string

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class WithSuffixKtTest {
    @Test
    internal fun `should append suffix if missing`() {
        expectThat("foo".withSuffix("bar")).isEqualTo("foobar")
    }

    @Test
    internal fun `should fully append suffix if partially missing`() {
        expectThat("foob".withSuffix("bar")).isEqualTo("foobbar")
    }

    @Test
    internal fun `should not append suffix if present`() {
        expectThat("foobar".withSuffix("bar")).isEqualTo("foobar")
    }
}
