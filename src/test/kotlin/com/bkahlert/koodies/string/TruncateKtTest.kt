package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(ExecutionMode.CONCURRENT)
class TruncateKtTest {
    @Test
    fun `should truncate`() {
        expectThat("12345678901234567890".truncate()).isEqualTo("12345678901234…")
    }

    @Test
    fun `should not truncate if not necessary`() {
        expectThat("1234567890".truncate()).isEqualTo("1234567890")
    }

    @Test
    fun `should truncate using custom marker`() {
        expectThat("12345678901234567890".truncate(marker = "...")).isEqualTo("123456789012...")
    }

    @Test
    fun `should throw if marker is longer than max length`() {
        expectCatching {
            "1234567890".truncate(maxLength = 1, marker = "XX")
        }.isFailure().isA<IllegalArgumentException>()
    }
}
