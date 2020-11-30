package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.LineSeparators.CR
import com.bkahlert.koodies.string.LineSeparators.CRLF
import com.bkahlert.koodies.string.LineSeparators.INTERMEDIARY_LINE_PATTERN
import com.bkahlert.koodies.string.LineSeparators.LAST_LINE_PATTERN
import com.bkahlert.koodies.string.LineSeparators.LINE_PATTERN
import com.bkahlert.koodies.string.LineSeparators.PS
import com.bkahlert.koodies.string.LineSeparators.SEPARATOR_PATTERN
import com.bkahlert.koodies.string.LineSeparators.firstLineSeparator
import com.bkahlert.koodies.string.LineSeparators.firstLineSeparatorLength
import com.bkahlert.koodies.string.LineSeparators.hasTrailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.isMultiline
import com.bkahlert.koodies.string.LineSeparators.lineSequence
import com.bkahlert.koodies.string.LineSeparators.lines
import com.bkahlert.koodies.string.LineSeparators.trailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.group
import com.imgcstmzr.util.groupValues
import com.imgcstmzr.util.matchEntire
import com.imgcstmzr.util.value
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNullOrEmpty
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class LineSeparatorsTest {

    @Nested
    inner class Multiline {

        @TestFactory
        fun `should detect multilines`() = listOf(
            "\n", "a\r\nb", "b\ra\nc", "sss\r",
        ).map { input ->
            dynamicTest("in ${input.debug}") {
                expectThat(input)
                    .isMultiline()
                    .not { isSingleLine() }
            }
        }

        @TestFactory
        fun `should detect single line`() = listOf(
            "", "b", "bds sd sd sds dac"
        ).map { input ->
            dynamicTest("in ${input.debug}") {
                expectThat(input)
                    .isSingleLine()
                    .not { isMultiline() }
            }
        }
    }

    @Test
    fun `should provide MAX_LENGTH`() {
        expectThat(LineSeparators.MAX_LENGTH).isEqualTo(2)
    }

    @Test
    fun `should iterate all line breaks in order`() {
        expectThat(LineSeparators.joinToString(" ") { "($it)" }).isEqualTo("(\r\n) (\n) (\r) (${LineSeparators.LS}) ($PS) (${LineSeparators.NL})")
    }

    @TestFactory
    fun lineOperations() = mapOf(
        "lineSequence" to fun(input: CharSequence, ignoreTrailSep: Boolean, keepDelimiters: Boolean): Iterable<String> =
            input.lineSequence(ignoreTrailingSeparator = ignoreTrailSep, keepDelimiters = keepDelimiters).toList(),
        "lines" to fun(input: CharSequence, ignoreTrailingSeparator: Boolean, keepDelimiters: Boolean): Iterable<String> =
            input.lines(ignoreTrailingSeparator = ignoreTrailingSeparator, keepDelimiters = keepDelimiters).toList(),
    ).map { (name: String, operation: CharSequence.(Boolean, Boolean) -> Iterable<String>) ->
        dynamicContainer("$name(", listOf(
            dynamicContainer("ignoreTrailingSeparator=false", false.let { keepTrailSep ->
                listOf(
                    dynamicContainer("keepDelimiters=false)", false.let { ignoreDelimiters ->
                        listOf(
                            dynamicTest("should keep trailing empty line if one existed and ignore delimiters") {
                                expectThat("1${CR}2${CRLF}3$PS".operation(keepTrailSep, ignoreDelimiters)).containsExactly("1", "2", "3", "")
                            },
                            dynamicTest("should not have trailing empty line if none existed and ignore delimiters") {
                                expectThat("1${CR}2${CRLF}3".operation(keepTrailSep, ignoreDelimiters)).containsExactly("1", "2", "3")
                            },
                            dynamicTest("should consist of single line if only one existed") {
                                expectThat("1".operation(keepTrailSep, ignoreDelimiters)).containsExactly("1")
                            },
                            dynamicTest("should be empty if only one is empty") {
                                expectThat("".operation(keepTrailSep, ignoreDelimiters)).containsExactly("")
                            },
                        )
                    }),
                    dynamicContainer("keepDelimiters=true)", true.let { keepDelimiters ->
                        listOf(
                            dynamicTest("should keep trailing empty line if one existed and keep delimiters") {
                                expectThat("1${CR}2${CRLF}3$PS".operation(keepTrailSep, keepDelimiters)).containsExactly("1$CR", "2$CRLF", "3$PS", "")
                            },
                            dynamicTest("should not have trailing empty line if none existed and keep delimiters") {
                                expectThat("1${CR}2${CRLF}3".operation(keepTrailSep, keepDelimiters)).containsExactly("1$CR", "2$CRLF", "3")
                            },
                            dynamicTest("should consist of single line if only one existed") {
                                expectThat("1".operation(keepTrailSep, keepDelimiters)).containsExactly("1")
                            },
                            dynamicTest("should be empty if only one is empty") {
                                expectThat("".operation(keepTrailSep, keepDelimiters)).containsExactly("")
                            },
                        )
                    }),
                )
            }),
            dynamicContainer("ignoreTrailingSeparator=true", true.let { ignoreTrailSep ->
                listOf(
                    dynamicContainer("keepDelimiters=false)", false.let { ignoreDelims ->
                        listOf(
                            dynamicTest("should not have trailing empty line if one existed and ignore delimiters") {
                                expectThat("1${CR}2${CRLF}3$PS".operation(ignoreTrailSep, ignoreDelims)).containsExactly("1", "2", "3")
                            },
                            dynamicTest("should not have trailing empty line if none existed and ignore delimiters") {
                                expectThat("1${CR}2${CRLF}3".operation(ignoreTrailSep, ignoreDelims)).containsExactly("1", "2", "3")
                            },
                            dynamicTest("should consist of single line if only one existed") {
                                expectThat("1".operation(ignoreTrailSep, ignoreDelims)).containsExactly("1")
                            },
                            dynamicTest("should be empty if only one is empty") {
                                expectThat("".operation(ignoreTrailSep, ignoreDelims)).containsExactly()
                            },
                        )
                    }),
                    dynamicContainer("keepDelimiters=true)", true.let { keepDelims ->
                        listOf(
                            dynamicTest("should not have trailing empty line if one existed and keep delimiters") {
                                expectThat("1${CR}2${CRLF}3$PS".operation(ignoreTrailSep, keepDelims)).containsExactly("1$CR", "2$CRLF", "3$PS")
                            },
                            dynamicTest("should not have trailing empty line if none existed and keep delimiters") {
                                expectThat("1${CR}2${CRLF}3".operation(ignoreTrailSep, keepDelims)).containsExactly("1$CR", "2$CRLF", "3")
                            },
                            dynamicTest("should consist of single line if only one existed") {
                                expectThat("1".operation(ignoreTrailSep, keepDelims)).containsExactly("1")
                            },
                            dynamicTest("should be empty if only one is empty") {
                                expectThat("".operation(ignoreTrailSep, keepDelims)).containsExactly()
                            },
                        )
                    }),
                )
            }),
        ))
    }

    @TestFactory
    fun `each line separator`() = LineSeparators.map { lineSeparator ->
        dynamicContainer(lineSeparator.replaceNonPrintableCharacters() + lineSeparator.debug, listOf(
            dynamicTest("should return itself") {
                expectThat(lineSeparator).isEqualTo(lineSeparator)
            },
            dynamicContainer("trailingLineSeparator()", listOf(
                dynamicTest("should return if present") {
                    expectThat("line$lineSeparator".trailingLineSeparator).isEqualTo(lineSeparator)
                },
                dynamicTest("should return null if missing") {
                    expectThat("line${lineSeparator}X".trailingLineSeparator).isNullOrEmpty()
                }
            )),
            dynamicContainer("trailing line separator", listOf(
                dynamicTest("should return if present") {
                    expectThat("line$lineSeparator".trailingLineSeparator).isEqualTo(lineSeparator)
                },
                dynamicTest("should return null if missing") {
                    expectThat("line${lineSeparator}X".trailingLineSeparator).isNullOrEmpty()
                }
            )),
            dynamicContainer("hasTrailingLineSeparator", listOf(
                dynamicTest("should return if present") {
                    expectThat("line$lineSeparator".hasTrailingLineSeparator).isTrue()
                },
                dynamicTest("should return null if missing") {
                    expectThat("line${lineSeparator}X".hasTrailingLineSeparator).isFalse()
                },
            )),
            dynamicContainer("withoutTrailingLineSeparator", listOf(
                dynamicTest("should return string with removed line separator if present") {
                    expectThat("line$lineSeparator".withoutTrailingLineSeparator).isEqualTo("line")
                },
                dynamicTest("should return unchanged if missing") {
                    expectThat("line${lineSeparator}X".withoutTrailingLineSeparator).isEqualTo("line${lineSeparator}X")
                },
            )),

            dynamicContainer("firstLineSeparatorAndLength", listOf(
                dynamicContainer("should return first line separator if present", listOf(
                    "at the beginning as single line separator" to "${lineSeparator}line",
                    "in the middle as single line separator" to "li${lineSeparator}ne",
                    "at the end as single line separator" to "line$lineSeparator",
                    "at the beginning" to "${lineSeparator}line${LineSeparators.joinToString()}",
                    "in the middle" to "li${lineSeparator}ne${LineSeparators.joinToString()}",
                    "at the end" to "line$lineSeparator${LineSeparators.joinToString()}",
                ).flatMap { (case, text) ->
                    listOf(
                        dynamicTest("$case (separator itself)") {
                            expectThat(text.firstLineSeparator).isEqualTo(lineSeparator)
                        },
                        dynamicTest("$case (separator's length)") {
                            expectThat(text.firstLineSeparatorLength).isEqualTo(lineSeparator.length)
                        },
                    )
                }),
                dynamicTest("should return 0 if single-line") {
                    expectThat("line".firstLineSeparatorLength).isEqualTo(0)
                },
            )),

            dynamicContainer(::SEPARATOR_PATTERN.name, listOf(
                dynamicTest("should not match empty string") {
                    expectThat(SEPARATOR_PATTERN).not { matchEntire("") }
                },
                dynamicTest("should match itself") {
                    expectThat(SEPARATOR_PATTERN).matchEntire(lineSeparator).groupValues.containsExactly(lineSeparator)
                },
                dynamicTest("should not match line$lineSeparator".replaceNonPrintableCharacters()) {
                    expectThat(SEPARATOR_PATTERN).not { matchEntire("line$lineSeparator") }
                },
            )),

            dynamicContainer(::LAST_LINE_PATTERN.name, listOf(
                dynamicTest("should not match empty string") {
                    expectThat(LAST_LINE_PATTERN).not { matchEntire("") }
                },
                dynamicTest("should match line") {
                    expectThat(LAST_LINE_PATTERN).matchEntire("line").groupValues.containsExactly("line")
                },
                dynamicTest("should not match line$lineSeparator".replaceNonPrintableCharacters()) {
                    expectThat(LAST_LINE_PATTERN).not { matchEntire("line$lineSeparator") }
                },
                dynamicTest("should not match line$lineSeparator...".replaceNonPrintableCharacters()) {
                    expectThat(LAST_LINE_PATTERN).not { matchEntire("line$lineSeparator...") }
                },
            )),

            dynamicContainer(::INTERMEDIARY_LINE_PATTERN.name, listOf(
                dynamicTest("should not match empty string") {
                    expectThat(INTERMEDIARY_LINE_PATTERN).not { matchEntire("") }
                },
                dynamicTest("should match itself") {
                    expectThat(INTERMEDIARY_LINE_PATTERN).matchEntire(lineSeparator).groupValues.containsExactly(lineSeparator, lineSeparator)
                },
                dynamicTest("should not match line") {
                    expectThat(INTERMEDIARY_LINE_PATTERN).not { matchEntire("line") }
                },
                dynamicTest("should match line$lineSeparator".replaceNonPrintableCharacters()) {
                    expectThat(INTERMEDIARY_LINE_PATTERN).matchEntire("line$lineSeparator").group("separator").value.isEqualTo(lineSeparator)
                },
                dynamicTest("should not match line$lineSeparator...".replaceNonPrintableCharacters()) {
                    expectThat(INTERMEDIARY_LINE_PATTERN).not { matchEntire("line$lineSeparator...") }
                },
            )),

            dynamicContainer(::LINE_PATTERN.name, listOf(
                dynamicTest("should not match empty string") {
                    expectThat(LINE_PATTERN).not { matchEntire("") }
                },
                dynamicTest("should match itself") {
                    expectThat(LINE_PATTERN).matchEntire(lineSeparator).groupValues.containsExactly(lineSeparator, lineSeparator)
                },
                dynamicTest("should match line") {
                    expectThat(LINE_PATTERN).matchEntire("line").groupValues.first().isEqualTo("line")
                },
                dynamicTest("should match line$lineSeparator".replaceNonPrintableCharacters()) {
                    expectThat(LINE_PATTERN).matchEntire("line$lineSeparator").group("separator").value.isEqualTo(lineSeparator)
                },
                dynamicTest("should not match line$lineSeparator...".replaceNonPrintableCharacters()) {
                    expectThat(LINE_PATTERN).not { matchEntire("line$lineSeparator...") }
                },
            )),
        ))
    }
}

fun <T : CharSequence> Assertion.Builder<T>.isMultiline() =
    assert("is multi line") {
        if (it.isMultiline) pass()
        else fail()
    }

fun <T : CharSequence> Assertion.Builder<T>.isSingleLine() =
    assert("is single line") {
        if (!it.isMultiline) pass()
        else fail()
    }

fun <T : CharSequence> Assertion.Builder<T>.lines(
    ignoreTrailingSeparator: Boolean = false,
    keepDelimiters: Boolean = false,
) = get("lines %s") { lines(ignoreTrailingSeparator = ignoreTrailingSeparator, keepDelimiters = keepDelimiters) }
