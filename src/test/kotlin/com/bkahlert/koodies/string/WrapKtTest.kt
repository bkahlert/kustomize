package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class WrapKtTest {

    @Test
    fun `should wrap string with independent strings`() {
        expectThat("text".wrap("<<", "➬")).isEqualTo("<<text➬")
    }

    @Test
    fun `should wrap string with equal strings`() {
        expectThat("text".wrap("⋆")).isEqualTo("⋆text⋆")
    }

    @Test
    fun `should wrap empty string with double quotes`() {
        expectThat("".wrap("⋆")).isEqualTo("⋆⋆")
    }

    @Test
    fun `should wrap null replacement character with double quotes on null`() {
        expectThat(null.wrap("⋆")).isEqualTo("⋆\u2400⋆")
    }
}
