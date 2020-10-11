@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class CutKtTest {
    @Test
    internal fun `should return left and right substring each from given pos `() {
        expectThat("Hello World!".cut(3u)).isEqualTo("Hel" to "lo World!")
    }

    @Test
    internal fun `should return empty left on 0`() {
        expectThat("Hello World!".cut(0u)).isEqualTo("" to "Hello World!")
    }

    @Test
    internal fun `should return empty left on length or above pos`() {
        expectThat("Hello World!".cut(1000_000u)).isEqualTo("Hello World!" to "")
    }
}