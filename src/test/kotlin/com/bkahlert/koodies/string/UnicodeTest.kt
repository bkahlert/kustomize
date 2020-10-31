package com.bkahlert.koodies.string

import com.bkahlert.koodies.number.ApproximationMode
import com.bkahlert.koodies.string.Unicode.Emojis.asEmoji
import com.bkahlert.koodies.string.Unicode.nextLine
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.util.isEqualToStringWise
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant

@Execution(CONCURRENT)
internal class UnicodeTest {
    @Nested
    inner class Get {

        @ConcurrentTestFactory
        internal fun `should return code point`() = listOf(
            133 to nextLine,
            119594 to Unicode.DivinationSymbols.tetragramForPurety,
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
    inner class Emojis {

        @ConcurrentTestFactory
        fun `maps hours`() = listOf(
            listOf(-12, 0, 12, 24) to listOf("🕛", "🕧"),
            listOf(-8, 4, 16) to listOf("🕓", "🕟"),
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
            Instant.parse("2020-02-02T02:02:02Z") to listOf("🕝", "🕑", "🕑"),
            Instant.parse("2020-02-02T22:32:02Z") to listOf("🕚", "🕥", "🕥"),
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
