package com.bkahlert.koodies.string

import com.bkahlert.koodies.number.ApproximationMode
import com.bkahlert.koodies.string.Unicode.DivinationSymbols.Tetragrams
import com.bkahlert.koodies.string.Unicode.Emojis.Emoji
import com.bkahlert.koodies.string.Unicode.Emojis.asEmoji
import com.bkahlert.koodies.string.Unicode.nextLine
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.util.isEqualToStringWise
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith
import java.time.Instant

@Execution(CONCURRENT)
internal class UnicodeTest {
    @Nested
    inner class Get {

        @ConcurrentTestFactory
        internal fun `should return code point`() = listOf(
            133 to nextLine,
            119594 to Tetragrams.Purity.toString(),
        ).flatMap { (codePoint, expected) ->
            listOf(
                DynamicTest.dynamicTest("\"$expected\" ？⃔ \"$codePoint\"") {
                    val actual: CodePoint = Unicode[codePoint]
                    expectThat(actual).isEqualToStringWise(expected)
                }
            )
        }
    }

    @Nested
    inner class UnicodeBlocks {
        @Test
        fun `should map one-char code point`() {
            expectThat("${Unicode.BoxDrawings.DownHeavyAndLeftLight}").isEqualTo("┒")
        }

        @Test
        fun `should map two-char code point`() {
            expectThat("${Tetragrams.Enlargement}").isEqualTo("𝌳")
        }

        @Test
        fun `should provide one-char code point table`() {
            expectThat(Unicode.BoxDrawings.values().first().asTable()).startsWith("""
                ─	BOX DRAWINGS LIGHT HORIZONTAL
                ━	BOX DRAWINGS HEAVY HORIZONTAL
                │	BOX DRAWINGS LIGHT VERTICAL
                ┃	BOX DRAWINGS HEAVY VERTICAL
            """.trimIndent())
        }

        @Test
        fun `should provide two-char code point table`() {
            expectThat(Unicode.DivinationSymbols.Tetragrams.values().first().asTable()).startsWith("""
                𝌆	TETRAGRAM FOR CENTRE
                𝌇	TETRAGRAM FOR FULL CIRCLE
                𝌈	TETRAGRAM FOR MIRED
                𝌉	TETRAGRAM FOR BARRIER
            """.trimIndent())
        }
    }


    @Nested
    inner class Emojis {

        @ConcurrentTestFactory
        fun `maps hours`() = listOf(
            listOf(-12, 0, 12, 24) to listOf(Emoji("🕛"), Emoji("🕧")),
            listOf(-8, 4, 16) to listOf(Emoji("🕓"), Emoji("🕟")),
        ).flatMap { (hours, expectations) ->
            hours.flatMap { hour ->
                listOf(
                    DynamicTest.dynamicTest("$hour:00 ➜ ${expectations[0]}") {
                        val actual = Unicode.Emojis.FullHoursDictionary[hour]
                        expectThat(actual).isEqualTo(expectations[0])
                    },
                    DynamicTest.dynamicTest("$hour:30 ➜ ${expectations[1]}") {
                        val actual = Unicode.Emojis.HalfHoursDictionary[hour]
                        expectThat(actual).isEqualTo(expectations[1])
                    },
                )
            }
        }

        @ConcurrentTestFactory
        fun `maps instants`() = listOf(
            Instant.parse("2020-02-02T02:02:02Z") to listOf(Emoji("🕝"), Emoji("🕑"), Emoji("🕑")),
            Instant.parse("2020-02-02T22:32:02Z") to listOf(Emoji("🕚"), Emoji("🕥"), Emoji("🕥")),
        ).flatMap { (instant, expectations) ->
            listOf(
                DynamicTest.dynamicTest("$instant rounded up to ${expectations[0]}") {
                    val actual = instant.asEmoji(ApproximationMode.Ceil)
                    expectThat(actual).isEqualTo(expectations[0])
                },
                DynamicTest.dynamicTest("$instant rounded down to ${expectations[1]}") {
                    val actual = instant.asEmoji(ApproximationMode.Floor)
                    expectThat(actual).isEqualTo(expectations[1])
                },
                DynamicTest.dynamicTest("$instant rounded to ${expectations[2]}") {
                    val actual = instant.asEmoji(ApproximationMode.Round)
                    expectThat(actual).isEqualTo(expectations[2])
                }
            )
        }
    }
}
