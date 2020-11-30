package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class UnquotedKtTest {

    @Test
    fun `should unquote double quoted string`() {
        expectThat("\"text\"".unquoted).isEqualTo("text")
    }

    @Test
    fun `should unquote single quoted string`() {
        expectThat("'text'".unquoted).isEqualTo("text")
    }

    @Test
    fun `should unquote wrapped empty string`() {
        expectThat("\"\"".unquoted).isEqualTo("")
    }

    @Test
    fun `should unquote wrapped null replacement character to empty string`() {
        expectThat("\"\u2400\"".unquoted).isEqualTo("")
    }

    @Test
    fun `should unquote multiply wrapped string only once`() {
        expectThat("\"\"text\"\"".unquoted).isEqualTo("\"text\"")
    }

    @Test
    fun `should not unquote on differing left and right`() {
        expectThat("\"text\'".unquoted).isEqualTo("\"text\'")
    }
}
