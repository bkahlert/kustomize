package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ESC
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLineSequence
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLines
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareMapLines
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiColors.black
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.strikethrough
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.underline
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
class AnsiCodeTest {

    val italicCyan = with(ANSI.termColors) { italic + cyan }
    val ansiFormattedString =
        italicCyan("${"Important:".underline()} This line has ${"no".strikethrough()} ANSI escapes.\nThis one's ${"bold!".bold()}\r\nLast one is clean.")
    val lines = listOf(
        "Important: This line has no ANSI escapes.",
        "This one's bold!",
        "Last one is clean.",
    )

    @Suppress("SpellCheckingInspection")
    val ansiFormattedLines = listOf(
        "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.",
        "$ESC[3;36mThis one's $ESC[1mbold!$ESC[22m$ESC[0m",
        "$ESC[3;36mLast one is clean.$ESC[23;39m$ESC[0m",
    )

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
            expectThat(ansiFormattedString.removeEscapeSequences().ansiAwareLineSequence().toList()).containsExactly(lines)
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiFormattedString.ansiAwareLineSequence().toList()).containsExactly(ansiFormattedLines)
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
            expectThat(ansiFormattedString.removeEscapeSequences().ansiAwareLines()).containsExactly(lines)
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiFormattedString.ansiAwareLines()).containsExactly(ansiFormattedLines)
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
