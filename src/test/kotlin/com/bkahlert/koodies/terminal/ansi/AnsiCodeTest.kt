package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ESC
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLength
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLineSequence
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLines
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareMapLines
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareSubstring
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiColors.black
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.strikethrough
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.underline
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.util.quoted
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(ExecutionMode.CONCURRENT)
class AnsiCodeTest {

    val italicCyan = with(ANSI.termColors) { italic + cyan }
    val ansiFormattedString =
        italicCyan("${"Important:".underline()} This line has ${"no".strikethrough()} ANSI escapes.\nThis one's ${"bold!".bold()}\r\nLast one is clean.")
    val expectedLines = listOf(
        "Important: This line has no ANSI escapes.",
        "This one's bold!",
        "Last one is clean.",
    )

    @Suppress("SpellCheckingInspection")
    val expectedAnsiFormattedLines = listOf(
        "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.",
        "$ESC[3;36mThis one's $ESC[1mbold!$ESC[22m$ESC[0m",
        "$ESC[3;36mLast one is clean.$ESC[23;39m$ESC[0m",
    )

    @Nested
    inner class AnsiAwareString {
        @ConcurrentTestFactory
        fun `should product right substring`(): List<DynamicTest> {
            return listOf(
                41 to "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.$ESC[23;39m",
                40 to "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes$ESC[23;39m",
                26 to "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;29;39m",
                11 to "$ESC[3;36m$ESC[4mImportant:$ESC[24m $ESC[23;39m",
                10 to "$ESC[3;36m$ESC[4mImportant:$ESC[23;24;39m",
                9 to "$ESC[3;36m$ESC[4mImportant$ESC[23;24;39m",
                0 to ""
            ).map { (expected, ansiString) ->
                dynamicTest("${ansiString.quoted}.length should be $expected") {
                    expectThat(ansiString.ansiAwareLength()).isEqualTo(expected)
                }
            }
        }
    }

    @Nested
    inner class AnsiAwareSubstring {
        val ansiAwareLength = 41
        val ansiString = expectedAnsiFormattedLines[0]

        @ConcurrentTestFactory
        fun `should product right substring`(): List<DynamicNode> {
            return listOf(
                ansiAwareLength to AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.", listOf(3, 36)),
                40 to AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes", listOf(3, 36)),
                25 to AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has ", listOf(3, 36)),
                11 to AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m ", listOf(3, 36)),
                10 to AnsiSubstring("$ESC[3;36m$ESC[4mImportant:", listOf(3, 36, 4)),
                9 to AnsiSubstring("$ESC[3;36m$ESC[4mImportant", listOf(3, 36, 4)),
                0 to AnsiSubstring("", emptyList()),
            ).map { (length, expected) ->
                DynamicContainer.dynamicContainer("$expected ...", listOf(
                    dynamicTest("should have subString(0, $length): \"$expected\"") {
                        expectThat(ansiString.ansiAwareSubstring(0, length)).isEqualTo(expected.toString())
                    },
                    dynamicTest("should have length $length") {
                        val substring = ansiString.ansiAwareSubstring(0, length).toString()
                        val actualLength = substring.ansiAwareLength()
                        expectThat(actualLength).isEqualTo(length)
                    },
                ))
            }
        }

        @ConcurrentTestFactory
        fun `should product right non zero start substring`(): List<DynamicNode> {
            return listOf(
                0 to AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has ", listOf(3, 36)),
                1 to AnsiSubstring("$ESC[3;36;4mmportant:$ESC[24m This line has ", listOf(3, 36)),
                9 to AnsiSubstring("$ESC[3;36;4m:$ESC[24m This line has ", listOf(3, 36)),
                10 to AnsiSubstring("$ESC[3;36;4m$ESC[24m This line has ", listOf(3, 36)),
                11 to AnsiSubstring("$ESC[3;36mThis line has ", listOf(3, 36)),
                25 to AnsiSubstring("$ESC[3;36m", listOf(3, 36)),
            ).map { (startIndex, expected) ->
                DynamicContainer.dynamicContainer("$expected ...", listOf(
                    dynamicTest("should have subString($startIndex, 25): \"$expected\"") {
                        expectThat(ansiString.ansiAwareSubstring(startIndex, 25)).isEqualTo(expected.toString())
                    },
                    dynamicTest("should have length ${25 - startIndex}") {
                        val substring = ansiString.ansiAwareSubstring(startIndex, 25).toString()
                        val actualLength = substring.ansiAwareLength()
                        expectThat(actualLength).isEqualTo(25 - startIndex)
                    },
                ))
            }
        }

        @ConcurrentTestFactory
        fun `should product right string`(): List<DynamicTest> {
            return listOf(
                AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.", listOf(3, 36)) to
                    "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.$ESC[23;39m",
                AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes", listOf(3, 36)) to
                    "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes$ESC[23;39m",
                AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has ", listOf(3, 36)) to
                    "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[23;39m",
                AnsiSubstring("$ESC[3;36m$ESC[4mImportant:$ESC[24m ", listOf(3, 36)) to
                    "$ESC[3;36m$ESC[4mImportant:$ESC[24m $ESC[23;39m",
                AnsiSubstring("$ESC[3;36m$ESC[4mImportant:", listOf(3, 4, 36)) to
                    "$ESC[3;36m$ESC[4mImportant:$ESC[23;24;39m",
                AnsiSubstring("$ESC[3;36m$ESC[4mImportant", listOf(3, 4, 36)) to
                    "$ESC[3;36m$ESC[4mImportant$ESC[23;24;39m",
                AnsiSubstring("", emptyList()) to
                    "",
            ).map { (substring, expected) ->
                dynamicTest("${substring.toString().quoted} should be equal to ${expected.quoted}") {
                    expectThat(substring.toString()).isEqualTo(expected)
                }
            }
        }

        @Test
        internal fun `should throw if length is beyond`() {
            expectCatching { ansiString.ansiAwareSubstring(0, ansiAwareLength + 1) }.isFailure().isA<IndexOutOfBoundsException>()
        }
    }

    @Test
    fun `should remove ANSI escapes`() {
        expectThat(ansiFormattedString.removeEscapeSequences()).isEqualTo("Important: This line has no ANSI escapes.\nThis one's bold!\r\nLast one is clean.")
    }

    @Nested
    inner class AnsiAwareMapLines {
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiFormattedString.removeEscapeSequences().ansiAwareMapLines { it.toUpperCase() }).isEqualTo("""
                IMPORTANT: THIS LINE HAS NO ANSI ESCAPES.
                THIS ONE'S BOLD!
                LAST ONE IS CLEAN.
            """.trimIndent())
        }

        @Test
        fun `should split ANSI string`() {
            @Suppress("SpellCheckingInspection")
            expectThat(ansiFormattedString.ansiAwareMapLines { it.toLowerCase() }).isEqualTo("""
                $ESC[3;36m$ESC[4mimportant:$ESC[24m this line has $ESC[9mno$ESC[29m ansi escapes.
                $ESC[3;36mthis one's $ESC[1mbold!$ESC[22m$ESC[0m
                $ESC[3;36mlast one is clean.$ESC[23;39m$ESC[0m
            """.trimIndent())
        }

        @Test
        fun `should skip errors`() {
            expectThat("$ESC[4;m ← missing second code $ESC[24m".ansiAwareMapLines { it.black() }
                .ansiAwareMapLines { it.replace("second", "second".magenta()) }).isEqualTo("$ESC[4;m ← missing $ESC[35msecond$ESC[39m code $ESC[24m")
        }
    }

    @Nested
    inner class AnsiAwareSequence {
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiFormattedString.removeEscapeSequences().ansiAwareLineSequence().toList()).containsExactly(expectedLines)
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiFormattedString.ansiAwareLineSequence().toList()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should skip errors`() {
            expectThat("$ESC[4;m ← missing second code $ESC[24m".ansiAwareLineSequence().toList()).containsExactly("$ESC[4;m ← missing second code $ESC[24m")
        }
    }

    @Nested
    inner class AnsiAwareLines {
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiFormattedString.removeEscapeSequences().ansiAwareLines()).containsExactly(expectedLines)
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiFormattedString.ansiAwareLines()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should skip errors`() {
            expectThat("$ESC[4;m ← missing second code $ESC[24m".ansiAwareLines()).containsExactly("$ESC[4;m ← missing second code $ESC[24m")
        }
    }

    @Suppress("SpellCheckingInspection", "LongLine")
    @ConcurrentTestFactory
    fun `stripping ANSI off of`() = listOf(
        "[$ESC[0;32m  OK  $ESC[0m] Listening on $ESC[0;1;39mudev Control Socket$ESC[0m." to
            "[  OK  ] Listening on udev Control Socket.",
        "Text" to "Text",
        "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___" to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___"
    ).flatMap { (formatted, expected) ->
        listOf(
            dynamicTest("\"$formatted\" should produce \"$expected\"") {
                expectThat(formatted.removeEscapeSequences<CharSequence>()).isEqualTo(expected)
            }
        )
    }
}
