package com.imgcstmzr.util

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Exec
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Suppress("RedundantInnerClassModifier")
@Execution(ExecutionMode.CONCURRENT)
internal class StringExtensionsKtTest {
    companion object {
        private const val ESC = '\u001B'
    }

    @TestFactory
    internal fun `stripping ANSI off of`() = listOf(
        "[$ESC[0;32m  OK  $ESC[0m] Listening on $ESC[0;1;39mudev Control Socket$ESC[0m." to
            "[  OK  ] Listening on udev Control Socket.",
        "Text" to "Text",
        "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___" to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___"
    ).flatMap { (formatted, expected) ->
        listOf(
            dynamicTest("\"$formatted\" should produce \"$expected\"") {
                expectThat(formatted.stripOffAnsi()).isEqualTo(expected)
            }
        )
    }

    @Nested
    inner class Contains {
        @TestFactory
        internal fun `NOT ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsiFormatting = false
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
        internal fun `NOT ignoring case AND ignoring ANSI`() = listOf(
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsiFormatting = true
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        internal fun `ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to false,
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
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to true,
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

    @Nested
    inner class NonPrintableCharacterReplacement {
        @Test
        internal fun `should replace non-printable with appropriate printable characters`() {
            @Suppress("InvisibleCharacter")
            val given = "abc --- ß ẞ ---                   \u200B     ␈ ␠ 　 〿 𝩿 𝪀 !" +
                " \u0000 \u0001 \u0002 \u0003 \u0004 \u0005 \u0006 \u0007 \u0008 \u0009 \u000A \u000B \u000C \u000D \u000E \u000F \u0010 \u0011 \u0012 \u0013 \u0014 \u0015 \u0016 \u0017 \u0018 \u0019 \u001A \u001B \u001C \u001D \u001E \u001F \u007F"

            val actual = given.replaceNonPrintableCharacters()

            expectThat(actual).isEqualTo("abc --- ❲LATIN SMALL LETTER SHARP S❳ ❲LATIN CAPITAL LETTER SHARP S❳ --- ❲EN SPACE❳ ❲EM SPACE❳ ❲THREE-PER-EM SPACE❳ ❲FOUR-PER-EM SPACE❳ ❲SIX-PER-EM SPACE❳ ❲FIGURE SPACE❳ ❲PUNCTUATION SPACE❳ ❲THIN SPACE❳ ❲HAIR SPACE❳ ❲ZERO WIDTH SPACE❳ ❲NARROW NO-BREAK SPACE❳ ❲MEDIUM MATHEMATICAL SPACE❳ ❲SYMBOL FOR BACKSPACE❳ ❲SYMBOL FOR SPACE❳ ❲IDEOGRAPHIC SPACE❳ ❲IDEOGRAPHIC HALF FILL SPACE❳ ❲\\ud836!!SURROGATE❳ ❲\\ud836!!SURROGATE❳ ! ␀ ␁ ␂ ␃ ␄ ␅ ␆ ␇ ␈ ␉ ⏎ ␋ ␌ ␍ ␎ ␏ ␐ ␑ ␒ ␓ ␔ ␕ ␖ ␗ ␘ ␙ ␚ ␛ ␜ ␝ ␞ ␟ ␡")
        }
    }

    @Test
    internal fun `should blend`() {
        val blended = "this is a test".blend('X')
        expectThat(blended).isEqualTo("XhXsXiX X XeXt")
    }

    @Test
    internal fun `should center string collection`() {
        val string = listOf("     foo", "  bar baz ")
        val actual = string.center('X')
        expectThat(actual).containsExactly("XXfooXX", "bar baz")
    }

    @Test
    internal fun `should center text`() {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        val actual = string.center('|')
        expectThat(actual).isEqualTo("""
            ||foo||
            bar baz
        """.trimIndent())
    }

    @Test
    internal fun `should border centered text`() {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        val actual = string.border("╭─╮\n│*│\n╰─╯", 0, 0)
        expectThat(actual).isEqualTo("""
            ╭───────╮
            │**foo**│
            │bar baz│
            ╰───────╯
        """.trimIndent())
    }

    @Test
    internal fun `should border centered text with padding and margin`() {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        val actual = string.border("╭─╮\n│*│\n╰─╯", padding = 2, margin = 4)
        expectThat(actual).isEqualTo("""
            *********************
            *********************
            ****╭───────────╮****
            ****│***********│****
            ****│****foo****│****
            ****│**bar baz**│****
            ****│***********│****
            ****╰───────────╯****
            *********************
            *********************
        """.trimIndent())
    }

    @Test
    internal fun `should show progress bar`() {
        Exec.Sync.execShellScript(workingDirectory = Path.of("").toAbsolutePath().resolve("src/main/resources"),
            customizer = { redirectOutput(System.out) }) { command("sh", "progressbar.sh") }
        (0..100).forEach {
            echo("\u0013" + "X".repeat(it % 10))
            Thread.sleep(50)
        }
    }
}
