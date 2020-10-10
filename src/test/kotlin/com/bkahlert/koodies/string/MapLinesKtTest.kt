package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class MapLinesKtTest {

    val transform = { s: String -> s + s.reversed() }

    @Test
    internal fun `should transform single line`() {
        expectThat("AB".mapLines(transform)).isEqualTo("ABBA")
    }

    @Test
    internal fun `should transform multi line`() {
        @Suppress("SpellCheckingInspection")
        expectThat("AB\nBA".mapLines(transform)).isEqualTo("ABBA\nBAAB")
    }

    @Test
    internal fun `should keep trailing line`() {
        expectThat("AB\nBA\n".mapLines { "X" }).isEqualTo("X\nX\n")
    }

    @Test
    internal fun `should map empty string`() {
        expectThat("".mapLines { "X" }).isEqualTo("X")
    }

    @Test
    internal fun `should map empty string and keep trailing line`() {
        expectThat("\n".mapLines { "X" }).isEqualTo("X\n")
    }
}
