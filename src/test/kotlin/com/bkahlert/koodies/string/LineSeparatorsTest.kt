package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.LineSeparators.firstLineSeparatorLength
import com.bkahlert.koodies.string.LineSeparators.hasTrailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.isMultiline
import com.bkahlert.koodies.string.LineSeparators.trailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.patch.isEqualTo
import com.imgcstmzr.util.debug
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNullOrEmpty
import strikt.assertions.isTrue

@Execution(CONCURRENT)
internal class LineSeparatorsTest {

    @Nested
    inner class Multiline {

        @ConcurrentTestFactory
        internal fun `should detect multilines`() = listOf(
            "\n", "a\r\nb", "b\ra\nc", "sss\r",
        ).map { input ->
            dynamicTest("in ${input.debug}") {
                expectThat(input.isMultiline).isTrue()
            }
        }

        @ConcurrentTestFactory
        internal fun `should detect single line`() = listOf(
            "", "b", "bds sd sd sds dac"
        ).map { input ->
            dynamicTest("in ${input.debug}") {
                expectThat(input.isMultiline).isFalse()
            }
        }
    }

    @Nested
    inner class CRLFSeparator {
        val lineSeparator = LineSeparators.CRLF

        @Test
        internal fun `should return carriage return and line feed character`() {
            expectThat(lineSeparator).isEqualTo("\r\n")
        }

        @Nested
        inner class TrailingLineSeparator {

            @Test
            internal fun `should return if present`() {
                expectThat("line$lineSeparator".trailingLineSeparator).isEqualTo(lineSeparator)
            }

            @Test
            internal fun `should return null if missing`() {
                expectThat("line${lineSeparator}X".trailingLineSeparator).isNullOrEmpty()
            }
        }

        @Nested
        inner class HasTrailingLineSeparator {

            @Test
            internal fun `should return if present`() {
                expectThat("line$lineSeparator".hasTrailingLineSeparator).isTrue()
            }

            @Test
            internal fun `should return null if missing`() {
                expectThat("line${lineSeparator}X".hasTrailingLineSeparator).isFalse()
            }
        }

        @Nested
        inner class WithoutTrailingLineSeparator {

            @Test
            internal fun `should return string with removed line separator if present`() {
                expectThat("line$lineSeparator".withoutTrailingLineSeparator).isEqualTo("line")
            }

            @Test
            internal fun `should return unchanged if missing`() {
                expectThat("line${lineSeparator}X".withoutTrailingLineSeparator).isEqualTo("line${lineSeparator}X")
            }
        }

        @Nested
        inner class FirstLineSeparatorLength {

            @ConcurrentTestFactory
            internal fun `should return first line separator length if present`() =
                listOf(
                    "at the beginning as single line separator" to "${lineSeparator}line",
                    "in the middle as single line separator" to "li${lineSeparator}ne",
                    "at the end as single line separator" to "line$lineSeparator",
                    "at the beginning" to "${lineSeparator}line${LineSeparators.joinToString()}",
                    "in the middle" to "li${lineSeparator}ne${LineSeparators.joinToString()}",
                    "at the end" to "line$lineSeparator${LineSeparators.joinToString()}",
                ).map { (case, text) ->
                    dynamicTest(case) {
                        expectThat(text.firstLineSeparatorLength).isEqualTo(2)
                    }
                }

            @Test
            internal fun `should return 0 if single-line`() {
                expectThat("line".firstLineSeparatorLength).isEqualTo(0)
            }
        }
    }

    @Nested
    inner class LFSeparator {
        val lineSeparator = LineSeparators.LF

        @Test
        internal fun `should return line feed character`() {
            expectThat(lineSeparator).isEqualTo("\n")
        }

        @Nested
        inner class TrailingLineSeparator {

            @Test
            internal fun `should return if present`() {
                expectThat("line$lineSeparator".trailingLineSeparator).isEqualTo(lineSeparator)
            }

            @Test
            internal fun `should return null if missing`() {
                expectThat("line${lineSeparator}X".trailingLineSeparator).isNullOrEmpty()
            }
        }

        @Nested
        inner class HasTrailingLineSeparator {

            @Test
            internal fun `should return if present`() {
                expectThat("line$lineSeparator".hasTrailingLineSeparator).isTrue()
            }

            @Test
            internal fun `should return null if missing`() {
                expectThat("line${lineSeparator}X".hasTrailingLineSeparator).isFalse()
            }
        }

        @Nested
        inner class WithoutTrailingLineSeparator {

            @Test
            internal fun `should return string with removed line separator if present`() {
                expectThat("line$lineSeparator".withoutTrailingLineSeparator).isEqualTo("line")
            }

            @Test
            internal fun `should return unchanged if missing`() {
                expectThat("line${lineSeparator}X".withoutTrailingLineSeparator).isEqualTo("line${lineSeparator}X")
            }
        }

        @Nested
        inner class FirstLineSeparatorLength {

            @ConcurrentTestFactory
            internal fun `should return first line separator length if present`() =
                listOf(
                    "at the beginning as single line separator" to "${lineSeparator}line",
                    "in the middle as single line separator" to "li${lineSeparator}ne",
                    "at the end as single line separator" to "line$lineSeparator",
                    "at the beginning" to "${lineSeparator}line${LineSeparators.joinToString()}",
                    "in the middle" to "li${lineSeparator}ne${LineSeparators.joinToString()}",
                    "at the end" to "line$lineSeparator${LineSeparators.joinToString()}",
                ).map { (case, text) ->
                    dynamicTest(case) {
                        expectThat(text.firstLineSeparatorLength).isEqualTo(1)
                    }
                }

            @Test
            internal fun `should return 0 if single-line`() {
                expectThat("line".firstLineSeparatorLength).isEqualTo(0)
            }
        }
    }

    @Nested
    inner class CRSeparator {
        val lineSeparator = LineSeparators.CR

        @Test
        internal fun `should return carriage return character`() {
            expectThat(lineSeparator).isEqualTo("\r")
        }

        @Nested
        inner class TrailingLineSeparator {

            @Test
            internal fun `should return if present`() {
                expectThat("line$lineSeparator".trailingLineSeparator).isEqualTo(lineSeparator)
            }

            @Test
            internal fun `should return null if missing`() {
                expectThat("line${lineSeparator}X".trailingLineSeparator).isNullOrEmpty()
            }
        }

        @Nested
        inner class HasTrailingLineSeparator {

            @Test
            internal fun `should return if present`() {
                expectThat("line$lineSeparator".hasTrailingLineSeparator).isTrue()
            }

            @Test
            internal fun `should return null if missing`() {
                expectThat("line${lineSeparator}X".hasTrailingLineSeparator).isFalse()
            }
        }

        @Nested
        inner class WithoutTrailingLineSeparator {

            @Test
            internal fun `should return string with removed line separator if present`() {
                expectThat("line$lineSeparator".withoutTrailingLineSeparator).isEqualTo("line")
            }

            @Test
            internal fun `should return unchanged if missing`() {
                expectThat("line${lineSeparator}X".withoutTrailingLineSeparator).isEqualTo("line${lineSeparator}X")
            }
        }

        @Nested
        inner class FirstLineSeparatorLength {

            @ConcurrentTestFactory
            internal fun `should return first line separator length if present`() =
                listOf(
                    "at the beginning as single line separator" to "${lineSeparator}line",
                    "in the middle as single line separator" to "li${lineSeparator}ne",
                    "at the end as single line separator" to "line$lineSeparator",
                    "at the beginning" to "${lineSeparator}line${LineSeparators.joinToString()}",
                    "in the middle" to "li${lineSeparator}ne${LineSeparators.joinToString()}",
                    "at the end" to "line$lineSeparator${LineSeparators.joinToString()}",
                ).map { (case, text) ->
                    dynamicTest(case) {
                        expectThat(text.firstLineSeparatorLength).isEqualTo(1)
                    }
                }

            @Test
            internal fun `should return 0 if single-line`() {
                expectThat("line".firstLineSeparatorLength).isEqualTo(0)
            }
        }
    }

    @Test
    internal fun `should provide MAX_LENGTH`() {
        expectThat(LineSeparators.MAX_LENGTH).isEqualTo(2)
    }

    @Test
    internal fun `should iterate all line breaks in order`() {
        expectThat(LineSeparators.joinToString(" ") { "($it)" }).isEqualTo("(\r\n) (\n) (\r)")
    }
}
