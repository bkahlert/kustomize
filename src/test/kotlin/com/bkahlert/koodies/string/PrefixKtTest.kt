package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class PrefixKtTest {
    @Test
    fun `should add prefix`() {
        expectThat("12345     12345".prefixWith("abc")).isEqualTo("abc12345     12345")
    }

    @Test
    fun `should do nothing on empty prefix`() {
        expectThat("12345     12345".prefixWith("")).isEqualTo("12345     12345")
    }
}
