package com.imgcstmzr.util

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class Slf4jExtensionsKtTest {

    @Nested
    inner class Replacement {

        @Test
        internal fun `should fill placeholders in SLF4J style format`() {
            val slf4jStyleFormat = "A {} C {} E"
            val actual = slf4jFormat(slf4jStyleFormat, "B", "D")
            expectThat(actual).isEqualTo("A B C D E")
        }

        @Test
        internal fun `should fill placeholders in SLF4J style format if too many args`() {
            val slf4jStyleFormat = "A {} C {} E"
            val actual = slf4jFormat(slf4jStyleFormat, "B", "D", "Z")
            expectThat(actual).isEqualTo("A B C D E")
        }

        @Test
        internal fun `should fill placeholders in SLF4J style format if too few args`() {
            val slf4jStyleFormat = "A {} C {} E"
            val actual = slf4jFormat(slf4jStyleFormat, "B")
            expectThat(actual).isEqualTo("A B C {1} E")
        }
    }
}
