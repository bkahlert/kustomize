package com.imgcstmzr

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class ToolsKtTest {
    @TestFactory
    internal fun `stripping ANSI off of`() = listOf(
        "[[0;32m  OK  [0m] Listening on [0;1;39mudev Control Socket[0m." to
            "[  OK  ] Listening on udev Control Socket.",
        "Text" to "Text",
        "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___" to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___"
    ).flatMap { (formatted, expected) ->
        listOf(
            dynamicTest("\"$formatted\" should produce \"$expected\"") {
                expectThat(stripOffAnsi(formatted)).isEqualTo(expected)
            }
        )
    }

    @Nested
    class Contains {
        @TestFactory
        internal fun `NOT ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[[0;32m  OK  [0m]" to "[[0;32m  OK") to true,
            ("[[0;32m  OK  [0m]" to "[[0;32m  ok") to false,
            ("[[0;32m  OK  [0m]" to "[  OK") to false,
            ("[[0;32m  OK  [0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsiFormatting = false
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        internal fun `NOT ignoring case AND ignoring ANSI`() = listOf(
            ("[[0;32m  OK  [0m]" to "[[0;32m  OK") to true,
            ("[[0;32m  OK  [0m]" to "[[0;32m  ok") to false,
            ("[[0;32m  OK  [0m]" to "[  OK") to true,
            ("[[0;32m  OK  [0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsiFormatting = true
                    )
                    expectThat(actual).isEqualTo(expected)
                },
                dynamicTest("should be default") {
                    val actual = input.first.contains(
                        input.second
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        internal fun `ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[[0;32m  OK  [0m]" to "[[0;32m  OK") to true,
            ("[[0;32m  OK  [0m]" to "[[0;32m  ok") to true,
            ("[[0;32m  OK  [0m]" to "[  OK") to false,
            ("[[0;32m  OK  [0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = true, ignoreAnsiFormatting = false
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        internal fun `ignoring case AND ignoring ANSI`() = listOf(
            ("[[0;32m  OK  [0m]" to "[[0;32m  OK") to true,
            ("[[0;32m  OK  [0m]" to "[[0;32m  ok") to true,
            ("[[0;32m  OK  [0m]" to "[  OK") to true,
            ("[[0;32m  OK  [0m]" to "[  ok") to true,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = true, ignoreAnsiFormatting = true
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }
    }
}
