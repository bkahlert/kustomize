package com.bkahlert.koodies.string

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo

@Execution(CONCURRENT)
class DecodeToValidStringKtTest {
    @Nested
    inner class EmptyString {
        @Test
        fun `should decode`() {
            expectThat(ByteArray(0).decodeToValidString()).isEmpty()
        }
    }

    @Nested
    inner class ValidString {
        val validString = "aβç".also { check(it.toByteArray().size == 5) }

        @Test
        fun `should decode`() {
            expectThat(validString.toByteArray().decodeToValidString()).isEqualTo(validString)
        }

        @Test
        fun `should not 'loose' any bytes`() {
            val byteArray = validString.toByteArray()
            expectThat(byteArray.decodeToValidString().toByteArray().size).isEqualTo(byteArray.size)
        }
    }

    @Nested
    inner class InvalidString {
        val invalidString = "aβ𝌔".toByteArray().dropLast(1).toByteArray()

        @Test
        fun `should convert array with invalid string`() {

            expectThat(invalidString.decodeToValidString()).isEqualTo("aβ")
        }

        @RepeatedTest(100)
        fun `should not 'loose' more than 3 bytes`() {
            expectThat("ab${Char.random}".toByteArray().dropLast(1).size - "aβ".toByteArray().size).isLessThanOrEqualTo(3)
        }
    }
}
