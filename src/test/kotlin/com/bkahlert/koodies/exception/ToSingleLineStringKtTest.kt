package com.bkahlert.koodies.exception

import com.bkahlert.koodies.terminal.removeEscapeSequences
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

@Execution(CONCURRENT)
internal class ToSingleLineStringKtTest {
    @Nested
    inner class AThrowable {

        @Test
        internal fun `should format as a single line`() {
            val actual = RuntimeException("test").toSingleLineString() ?: ""
            expectThat(actual).startsWith("java.lang.RuntimeException: test @ com.bkahlert.koodies.exception.ToSingleLineStringKtTest")
            expectThat(actual.lines()).hasSize(1)
        }
    }

    @Nested
    inner class AResult {

        @Nested
        inner class WithValue {

            @Test
            internal fun `should format as a single line`() {
                val actual = Result.success("good").toSingleLineString() ?: ""
                expectThat(actual.removeEscapeSequences()).isEqualTo("❬good⫻4❭")
                expectThat(actual.lines()).hasSize(1)
            }
        }


        @Nested
        inner class WithException {

            @Test
            internal fun `should format as a single line`() {
                val actual = Result.failure<String>(RuntimeException("test")).toSingleLineString() ?: ""
                expectThat(actual).startsWith("java.lang.RuntimeException: test @ com.bkahlert.koodies.exception.ToSingleLineStringKtTest")
                expectThat(actual.lines()).hasSize(1)
            }
        }
    }
}
