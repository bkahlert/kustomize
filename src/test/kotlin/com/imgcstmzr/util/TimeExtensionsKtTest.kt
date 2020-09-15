package com.imgcstmzr.util

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class TimeExtensionsKtTest {

    @Nested
    inner class ClockMapping {

        @TestFactory
        internal fun `maps hours`() = listOf(
            listOf(-12, 0, 12, 24) to listOf("🕛", "🕧"),
            listOf(-8, 4, 16) to listOf("🕓", "🕟"),
        ).flatMap { (hours, expectations) ->
            hours.flatMap { hour ->
                listOf(
                    dynamicTest("$hour:00 ➜ ${expectations[0]}") {
                        val actual = FullHourClockEmojisDictionary[hour]
                        expectThat(actual).isEqualTo(expectations[0])
                    },
                    dynamicTest("$hour:30 ➜ ${expectations[1]}") {
                        val actual = HalfHourClockEmojisDictionary[hour]
                        expectThat(actual).isEqualTo(expectations[1])
                    },
                )
            }
        }

        @TestFactory
        internal fun `maps instants`() = listOf(
            Instant.parse("2020-02-02T02:02:02Z") to listOf("🕝", "🕑", "🕑"),
            Instant.parse("2020-02-02T22:32:02Z") to listOf("🕚", "🕥", "🕥"),
        ).flatMap { (instant, expectations) ->
            listOf(
                dynamicTest("$instant rounded up to ${expectations[0]}") {
                    val actual = instant.asEmoji(ApproximationMode.Ceil)
                    expectThat(actual).isEqualTo(expectations[0])
                },
                dynamicTest("$instant rounded down to ${expectations[1]}") {
                    val actual = instant.asEmoji(ApproximationMode.Floor)
                    expectThat(actual).isEqualTo(expectations[1])
                },
                dynamicTest("$instant rounded to ${expectations[2]}") {
                    val actual = instant.asEmoji(ApproximationMode.Round)
                    expectThat(actual).isEqualTo(expectations[2])
                }
            )
        }
    }
}

