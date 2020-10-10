package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class ContainsKtTest {

    @ConcurrentTestFactory
    internal fun `NOT ignoring case AND NOT ignoring ANSI`() = listOf(
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  OK") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  ok") to false,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  OK") to false,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  ok") to false,
    ).flatMap { (input, expected) ->
        listOf(
            DynamicTest.dynamicTest("$input > $expected") {
                val actual = input.first.contains(
                    input.second, ignoreCase = false, ignoreAnsiFormatting = false
                )
                expectThat(actual).isEqualTo(expected)
            },
            DynamicTest.dynamicTest("should be default") {
                val actual = input.first.contains(
                    input.second
                )
                expectThat(actual).isEqualTo(expected)
            }
        )
    }

    @ConcurrentTestFactory
    internal fun `NOT ignoring case AND ignoring ANSI`() = listOf(
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  OK") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  ok") to false,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  OK") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  ok") to false,
    ).flatMap { (input, expected) ->
        listOf(
            DynamicTest.dynamicTest("$input > $expected") {
                val actual = input.first.contains(
                    input.second, ignoreCase = false, ignoreAnsiFormatting = true
                )
                expectThat(actual).isEqualTo(expected)
            }
        )
    }

    @ConcurrentTestFactory
    internal fun `ignoring case AND NOT ignoring ANSI`() = listOf(
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  OK") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  ok") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  OK") to false,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  ok") to false,
    ).flatMap { (input, expected) ->
        listOf(
            DynamicTest.dynamicTest("$input > $expected") {
                val actual = input.first.contains(
                    input.second, ignoreCase = true, ignoreAnsiFormatting = false
                )
                expectThat(actual).isEqualTo(expected)
            }
        )
    }

    @ConcurrentTestFactory
    internal fun `ignoring case AND ignoring ANSI`() = listOf(
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  OK") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[${ANSI.EscapeSequences.ESC}[0;32m  ok") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  OK") to true,
        ("[${ANSI.EscapeSequences.ESC}[0;32m  OK  ${ANSI.EscapeSequences.ESC}[0m]" to "[  ok") to true,
    ).flatMap { (input, expected) ->
        listOf(
            DynamicTest.dynamicTest("$input > $expected") {
                val actual = input.first.contains(
                    input.second, ignoreCase = true, ignoreAnsiFormatting = true
                )
                expectThat(actual).isEqualTo(expected)
            }
        )
    }
}
