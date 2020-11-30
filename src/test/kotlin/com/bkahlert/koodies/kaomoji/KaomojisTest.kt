package com.bkahlert.koodies.kaomoji

import com.bkahlert.koodies.kaomoji.Kaomojis.fishing
import com.bkahlert.koodies.kaomoji.Kaomojis.thinking
import com.bkahlert.koodies.string.codePointSequence
import com.bkahlert.koodies.terminal.IDE
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.hidden
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class KaomojisTest {

    @TestFactory
    fun `should create random Kaomoji`() = (0 until 10).map { i ->
        val kaomoji = Kaomojis.random()
        dynamicTest(kaomoji) {
            expectThat(kaomoji).get { codePointSequence().count() }.isGreaterThanOrEqualTo(3)
        }
    }

    @TestFactory
    fun `should create random Kaomoji from`() = Kaomojis.Generator.values().map { category ->
        dynamicContainer(category.name, (0 until 10).map { i ->
            val kaomoji = category.random()
            dynamicTest(kaomoji) {
                expectThat(kaomoji).get { codePointSequence().count() }.isGreaterThanOrEqualTo(3)
            }
        })
    }

    @TestFactory
    fun `should create random dogs`() = (0 until 10).map { i ->
        val kaomoji = Kaomojis.Dogs.random()
        dynamicTest(kaomoji) {
            expectThat(kaomoji).get { length }.isGreaterThanOrEqualTo(5)
        }
    }

    @TestFactory
    fun `should create random wizards`() = (0 until 10).map { i ->
        val kaomoji = Kaomojis.`(＃￣_￣)o︠・━・・━・━━・━☆`.random()
        dynamicTest(kaomoji) {
            expectThat(kaomoji).get { length }.isGreaterThanOrEqualTo(5)
        }
    }

    @Nested
    inner class RandomThinkingKaomoji {
        @Test
        fun `should create thinking Kaomoji`() {
            val hidden = if (IDE.isIntelliJ) "    " else "・㉨・".hidden()
            expectThat(Kaomojis.Bear[0].thinking("oh no")).isEqualTo("""
                $hidden   ͚͔˱ ❨ ( oh no )
                ・㉨・ ˙
            """.trimIndent())
        }
    }

    @Nested
    inner class RandomFishingKaomoji {
        @Test
        fun `should be created with random fisher and specified fish`() {
            expectThat(fishing("❮°«⠶＞˝")).endsWith("o/￣￣￣❮°«⠶＞˝")
        }

        @Test
        fun `should be created with specified fisher and random fish`() {
            expectThat(Kaomojis.Shrug[0].fishing()).startsWith("┐(´д｀)o/￣￣￣")
        }
    }
}
