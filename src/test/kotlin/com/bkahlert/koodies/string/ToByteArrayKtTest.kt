package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class ToByteArrayKtTest {
    @Test
    internal fun `should produce same byte array as string with default encoding`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat(StringBuilder(string).toByteArray()).isEqualTo(string.toByteArray())
    }

    @Test
    internal fun `should produce same byte array as string with explicit encoding`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat(StringBuilder(string).toByteArray(Charsets.UTF_16LE)).isEqualTo(string.toByteArray(Charsets.UTF_16LE))
    }
}
