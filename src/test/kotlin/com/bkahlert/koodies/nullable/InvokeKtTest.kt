package com.bkahlert.koodies.nullable

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class InvokeKtTest {

    @Nested
    inner class WithNonNullableReturnType {

        @Test
        internal fun `should apply f if set`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String)? = { it + it }
            expectThat(f("a")).isEqualTo("aa")
        }

        @Test
        internal fun `should return unchanged argument if f is unset`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String)? = null
            expectThat(f("a")).isEqualTo("a")
        }
    }

    @Nested
    inner class WithNullableReturnType {

        @Test
        internal fun `should return result of applied f if non-null returned`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String?)? = { it + it }
            expectThat("a".letIfSet(f)).isEqualTo("aa")
        }

        @Test
        internal fun `should return unchanged argument if null returned`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String?)? = { null }
            expectThat("a".letIfSet(f)).isEqualTo("a")
        }

        @Test
        internal fun `should return unchanged argument if f is unset`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String)? = null
            expectThat("a".letIfSet(f)).isEqualTo("a")
        }
    }
}
