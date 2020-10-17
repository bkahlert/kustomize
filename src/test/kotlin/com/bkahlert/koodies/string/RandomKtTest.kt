package com.bkahlert.koodies.string

import com.bkahlert.koodies.terminal.ascii.Kaomojis
import com.imgcstmzr.util.containsOnlyCharacters
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.doesNotContain
import strikt.assertions.hasLength
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo


@Execution(ExecutionMode.CONCURRENT)
internal class RandomKtTest {

    @Test
    internal fun `should have 16 length by default`() {
        expectThat(String.random()).hasLength(16)
    }

    @Test
    internal fun `should have allow variable length`() {
        expectThat(String.random(7)).hasLength(7)
    }

    @Test
    internal fun `should only alphanumeric on default`() {
        expectThat(String.random(10000)).containsOnlyCharacters(String.random.alphanumericCharacters)
    }

    @Test
    internal fun `should not easily produce the same string`(logger: InMemoryLogger<Unit>) {
        val calculated = mutableListOf<String>()
        (0 until 10000).onEach {
            calculated += String.random(8).also {
                logger.log(Kaomojis.`(＃￣_￣)o︠・━・・━・━━・━☆`.toString() + " " + it, true)
                expectThat(calculated).doesNotContain(it)
            }
        }
        expectThat(calculated).hasSize(10000)
    }

    @Test
    internal fun `should allow different character ranges`() {
        expectThat(String.random(1000, charArrayOf('A', 'B'))).containsOnlyCharacters(charArrayOf('A', 'B'))
    }

    @Test
    internal fun `should create crypt salt`() {
        expectThat(String.random.cryptSalt())
            .hasLength(2)
            .containsOnlyCharacters(String.random.alphanumericCharacters)
    }

    @RepeatedTest(100)
    internal fun `should create valid random CodePoint`() {
        expectThat(CodePoint.random.toString().codePoints().count()).isEqualTo(1)
    }

    @RepeatedTest(100)
    internal fun `should create valid random single character string`() {
        expectThat(Char.random.codePoints().count()).isEqualTo(1)
    }
}
