package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class UnwrapKtTest {

    @Test
    fun `should unwrap wrapped string`() {
        expectThat("\"text\"".unwrap("\"")).isEqualTo("text")
    }

    @Test
    fun `should unwrap wrapped empty string`() {
        expectThat("\"\"".unwrap("\"")).isEqualTo("")
    }

    @Test
    fun `should unwrap wrapped null replacement character to empty string`() {
        expectThat("\"␀\"".unwrap("\"")).isEqualTo("")
    }

    @Test
    fun `should unwrap multiply wrapped string`() {
        @Suppress("SpellCheckingInspection")
        expectThat("äöütextüöä".unwrap("ä", "ö", "ü")).isEqualTo("text")
    }

    @Test
    fun `should unwrap outside to inside`() {
        @Suppress("SpellCheckingInspection")
        expectThat("äöütextüöä".unwrap("ü", "ö", "ä")).isEqualTo("öütextüö")
    }

    @Test
    fun `should not unwrap on differing left and right`() {
        expectThat("\"text\'".unwrap("\"")).isEqualTo("\"text\'")
    }
}
