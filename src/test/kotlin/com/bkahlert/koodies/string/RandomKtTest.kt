package com.bkahlert.koodies.string

import com.bkahlert.koodies.kaomoji.Kaomojis
import com.imgcstmzr.util.containsOnlyCharacters
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.doesNotContain
import strikt.assertions.hasLength
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class RandomKtTest {

    @Test
    fun `should have 16 length by default`() {
        expectThat(String.random()).hasLength(16)
    }

    @Test
    fun `should have allow variable length`() {
        expectThat(String.random(7)).hasLength(7)
    }

    @Test
    fun `should only alphanumeric on default`() {
        expectThat(String.random(10000)).containsOnlyCharacters(String.random.alphanumericCharacters)
    }

    @Test
    fun `should not easily produce the same string`(logger: InMemoryLogger<Unit>) {
        val calculated = mutableListOf<String>()
        (0 until 1000).onEach {
            calculated += String.random(8).also {
                logger.logLine { Kaomojis.`(＃￣_￣)o︠・━・・━・━━・━☆`.toString() + " " + it }
                expectThat(calculated).doesNotContain(it)
            }
        }
        expectThat(calculated).hasSize(1000)
    }

    @Test
    fun `should allow different character ranges`() {
        expectThat(String.random(1000, charArrayOf('A', 'B'))).containsOnlyCharacters(charArrayOf('A', 'B'))
    }

    @Test
    fun `should create crypt salt`() {
        expectThat(String.random.cryptSalt())
            .hasLength(2)
            .containsOnlyCharacters(String.random.alphanumericCharacters)
    }

    @RepeatedTest(100)
    fun `should create valid random CodePoint`() {
        expectThat(CodePoint.random.toString().codePoints().count()).isEqualTo(1)
    }

    @RepeatedTest(100)
    fun `should create valid random single character string`() {
        expectThat(Char.random.codePoints().count()).isEqualTo(1)
    }
}
