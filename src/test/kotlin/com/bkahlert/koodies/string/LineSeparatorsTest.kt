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
import com.imgcstmzr.patch.isEqualTo
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.namedGroups
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
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
                expectThat(input.isMultiline).isTrue()
            }
        }

        @TestFactory
        fun `should detect single line`() = listOf(
            "", "b", "bds sd sd sds dac"
        ).map { input ->
            dynamicTest("in ${input.debug}") {
                expectThat(input.isMultiline).isFalse()
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

    @Nested
    inner class LineSequence {

        @Test
        fun `should keep trailing new lines`() {
            expectThat("1${CR}2${CRLF}3$PS".lineSequence().toList())
                .containsExactly("1", "2", "3", "")
        }

        @Nested
        inner class WithTrailingSeparatorIgnored {
            @Test
            fun `should not have trailing empty line if one existed`() {
                expectThat("1${CR}2${CRLF}3$PS".lineSequence(ignoreTrailingSeparator = true).toList())
                    .containsExactly("1", "2", "3")
            }

            @Test
            fun `should not have trailing empty line if none existed`() {
                expectThat("1${CR}2${CRLF}3".lineSequence(ignoreTrailingSeparator = true).toList())
                    .containsExactly("1", "2", "3")
            }

            @Test
            fun `should consist of single line if only one existed`() {
                expectThat("1".lineSequence(ignoreTrailingSeparator = true).toList())
                    .containsExactly("1")
            }

            @Test
            fun `should be empty if only (trailing) line is empty`() {
                expectThat("".lineSequence(ignoreTrailingSeparator = true).toList())
                    .isEmpty()
            }
        }
    }

    @Nested
    inner class Lines {

        @Test
        fun `should keep trailing new lines`() {
            expectThat("1${CR}2${CRLF}3$PS".lines())
                .containsExactly("1", "2", "3", "")
        }

        @Nested
        inner class WithTrailingSeparatorIgnored {
            @Test
            fun `should not have trailing empty line if one existed`() {
                expectThat("1${CR}2${CRLF}3$PS".lines(ignoreTrailingSeparator = true))
                    .containsExactly("1", "2", "3")
            }

            @Test
            fun `should not have trailing empty line if none existed`() {
                expectThat("1${CR}2${CRLF}3".lines(ignoreTrailingSeparator = true))
                    .containsExactly("1", "2", "3")
            }

            @Test
            fun `should consist of single line if only one existed`() {
                expectThat("1".lines(ignoreTrailingSeparator = true))
                    .containsExactly("1")
            }

            @Test
            fun `should be empty if only (trailing) line is empty`() {
                expectThat("".lineSequence(ignoreTrailingSeparator = true).toList())
                    .isEmpty()
            }
        }
    }

    @TestFactory
    fun `all line separators`() = LineSeparators.map { lineSeparator ->
        dynamicContainer(lineSeparator.replaceNonPrintableCharacters(), listOf(
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

            dynamicContainer("SEPARATOR_PATTERN", listOf(
                dynamicTest("should not match empty string") {
                    val match = SEPARATOR_PATTERN.matchEntire("")
                    expectThat(match).isNull()
                },
                dynamicTest("should match itself") {
                    val match = SEPARATOR_PATTERN.matchEntire(lineSeparator)
                    expectThat(match?.groupValues).isNotNull().containsExactly(lineSeparator)
                },
                dynamicTest("should not match " + "line$lineSeparator".replaceNonPrintableCharacters()) {
                    val match = SEPARATOR_PATTERN.matchEntire("line$lineSeparator")
                    expectThat(match?.groupValues).isNull()
                },
            )),

            dynamicContainer("LAST_LINE_PATTERN", listOf(
                dynamicTest("should not match empty string") {
                    val match = LAST_LINE_PATTERN.matchEntire("")
                    expectThat(match).isNull()
                },
                dynamicTest("should match line") {
                    val match = LAST_LINE_PATTERN.matchEntire("line")
                    expectThat(match).get { this?.groupValues }.isNotNull().containsExactly("line")
                },
                dynamicTest("should not match " + "line$lineSeparator".replaceNonPrintableCharacters()) {
                    val match = LAST_LINE_PATTERN.matchEntire("line$lineSeparator")
                    expectThat(match).isNull()
                },
                dynamicTest("should not match" + "line$lineSeparator...".replaceNonPrintableCharacters()) {
                    val match = LAST_LINE_PATTERN.matchEntire("line$lineSeparator...")
                    expectThat(match?.groupValues).isNull()
                },
            )),

            dynamicContainer("INTERMEDIARY_LINE_PATTERN", listOf(
                dynamicTest("should not match empty string") {
                    val match = INTERMEDIARY_LINE_PATTERN.matchEntire("")
                    expectThat(match).isNull()
                },
                dynamicTest("should match itself") {
                    val match = INTERMEDIARY_LINE_PATTERN.matchEntire(lineSeparator)
                    expectThat(match?.groupValues).isNotNull().containsExactly(lineSeparator, lineSeparator)
                },
                dynamicTest("should not match line") {
                    val match = INTERMEDIARY_LINE_PATTERN.matchEntire("line")
                    expectThat(match).isNull()
                },
                dynamicTest("should match " + "line$lineSeparator".replaceNonPrintableCharacters()) {
                    val match = INTERMEDIARY_LINE_PATTERN.matchEntire("line$lineSeparator")
                    expectThat(match)
                        .get { this?.namedGroups }
                        .get { this?.get("separator") }
                        .get { this?.value }
                        .isEqualTo(lineSeparator)
                },
                dynamicTest("should not match" + "line$lineSeparator...".replaceNonPrintableCharacters()) {
                    val match = INTERMEDIARY_LINE_PATTERN.matchEntire("line$lineSeparator...")
                    expectThat(match?.groupValues).isNull()
                },
            )),

            dynamicContainer("LINE_PATTERN", listOf(
                dynamicTest("should not match empty string") {
                    val match = LINE_PATTERN.matchEntire("")
                    expectThat(match).isNull()
                },
                dynamicTest("should match itself") {
                    val match = LINE_PATTERN.matchEntire(lineSeparator)
                    expectThat(match?.groupValues).isNotNull().containsExactly(lineSeparator, lineSeparator)
                },
                dynamicTest("should match line") {
                    val match = LINE_PATTERN.matchEntire("line")
                    expectThat(match)
                        .get { this?.groups }
                        .get { this?.get(0) }
                        .get { this?.value }
                        .isEqualTo("line")
                },
                dynamicTest("should match " + "line$lineSeparator".replaceNonPrintableCharacters()) {
                    val match = LINE_PATTERN.matchEntire("line$lineSeparator")
                    expectThat(match)
                        .get { this?.namedGroups }
                        .get { this?.get("separator") }
                        .get { this?.value }
                        .isEqualTo(lineSeparator)
                },
                dynamicTest("should not match" + "line$lineSeparator...".replaceNonPrintableCharacters()) {
                    val match = LINE_PATTERN.matchEntire("line$lineSeparator...")
                    expectThat(match?.groupValues).isNull()
                },
            )),
        ))
    }
}
