package com.bkahlert.koodies.string

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class TruncationStrategyTest {

    @Nested
    inner class StartTruncation {

        @ConcurrentTestFactory
        fun `should truncate`() = listOf(
            "APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace" to "…AvailableSpace",
            "A pretty long sentence works, too." to "…ce works, too.",
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                    val actual = input.truncate(strategy = TruncationStrategy.START)
                    expectThat(actual).isEqualTo(expected)
                },
                dynamicTest("\"$expected\" ？⃔ length(15)") {
                    val actual = input.truncate(strategy = TruncationStrategy.START)
                    expectThat(actual).hasLength(15)
                },
            )
        }

        @Test
        fun `should not truncate if not needed`() {
            val actual = "Too short".truncate(strategy = TruncationStrategy.START)
            expectThat(actual).isEqualTo("Too short")
        }
    }

    @Nested
    inner class MiddleTruncation {

        @ConcurrentTestFactory
        fun `should truncate`() = listOf(
            "APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace" to "APretty…leSpace",
            "A pretty long sentence works, too." to "A prett…s, too.",
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                    val actual = input.truncate(strategy = TruncationStrategy.MIDDLE)
                    expectThat(actual).isEqualTo(expected)
                },
                dynamicTest("\"$expected\" ？⃔ length(15)") {
                    val actual = input.truncate(strategy = TruncationStrategy.MIDDLE)
                    expectThat(actual).hasLength(15)
                },
            )
        }

        @Test
        fun `should not truncate if not needed`() {
            val actual = "Too short".truncate(strategy = TruncationStrategy.MIDDLE)
            expectThat(actual).isEqualTo("Too short")
        }
    }

    @Nested
    inner class EndTruncation {

        @ConcurrentTestFactory
        fun `should truncate`() = listOf(
            "APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace" to "APrettyLongCla…",
            "A pretty long sentence works, too." to "A pretty long …",
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                    val actual = input.truncate(strategy = TruncationStrategy.END)
                    expectThat(actual).isEqualTo(expected)
                },
                dynamicTest("\"$expected\" ？⃔ length(15)") {
                    val actual = input.truncate(strategy = TruncationStrategy.END)
                    expectThat(actual).hasLength(15)
                },
            )
        }

        @Test
        fun `should not truncate if not needed`() {
            val actual = "Too short".truncate(strategy = TruncationStrategy.END)
            expectThat(actual).isEqualTo("Too short")
        }
    }

    @Test
    fun `should truncate to max 15 at the end of the string using ellipsis`() {
        expectThat("APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace".truncate()).isEqualTo("APrettyLongCla…")
    }
}
