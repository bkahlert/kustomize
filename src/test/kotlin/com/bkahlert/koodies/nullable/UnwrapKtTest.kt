package com.bkahlert.koodies.nullable

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.util.Optional

@Execution(CONCURRENT)
internal class UnwrapKtTest {

    @Nested
    inner class AnOptional {
        @Test
        internal fun `should unwrap present value`() {
            val optional: Optional<String> = Optional.of("test")
            val unwrapped: String? = optional.unwrap
            expectThat(unwrapped).isEqualTo("test")
        }

        @Test
        internal fun `should unwrap non-present value`() {
            val optional: Optional<String> = Optional.empty()
            val unwrapped: String? = optional.unwrap
            expectThat(unwrapped).isNull()
        }
    }

    @Nested
    inner class ANullableOptional {
        @Test
        internal fun `should unwrap present value`() {
            @Suppress("RedundantNullableReturnType")
            val optional: Optional<String>? = Optional.of("test")
            val unwrapped: String? = optional.unwrap
            expectThat(unwrapped).isEqualTo("test")
        }

        @Test
        internal fun `should unwrap non-present value`() {
            @Suppress("RedundantNullableReturnType")
            val optional: Optional<String>? = Optional.empty()
            val unwrapped: String? = optional.unwrap
            expectThat(unwrapped).isNull()
        }

        @Test
        internal fun `should unwrap null optional`() {
            val optional: Optional<String>? = null
            val unwrapped: String? = optional.unwrap
            expectThat(unwrapped).isNull()
        }
    }
}
