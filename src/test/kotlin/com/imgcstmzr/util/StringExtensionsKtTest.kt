package com.imgcstmzr.util

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.process.Exec
import com.imgcstmzr.process.Output.Type.OUT
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
internal class StringExtensionsKtTest {

    @Nested
    inner class NonPrintableCharacterReplacement {
        @Test
        internal fun `should replace non-printable with appropriate printable characters`() {
            @Suppress("InvisibleCharacter")
            val given = "abc --- ß | ẞ ╭─═╗ ---                   \u200B     ␈ ␠ 　 〿 𝩿 𝪀 !" +
                " \u0000 \u0001 \u0002 \u0003 \u0004 \u0005 \u0006 \u0007 \u0008 \u0009 \u000A \u000B \u000C \u000D \u000E \u000F \u0010 \u0011 \u0012 \u0013 \u0014 \u0015 \u0016 \u0017 \u0018 \u0019 \u001A \u001B \u001C \u001D \u001E \u001F \u007F"

            val actual = given.replaceNonPrintableCharacters()

            expectThat(actual).isEqualTo("abc --- ❲LATIN SMALL LETTER SHARP S❳ | ❲LATIN CAPITAL LETTER SHARP S❳ ╭─═╗ --- ❲EN SPACE❳ ❲EM SPACE❳ ❲THREE-PER-EM SPACE❳ ❲FOUR-PER-EM SPACE❳ ❲SIX-PER-EM SPACE❳ ❲FIGURE SPACE❳ ❲PUNCTUATION SPACE❳ ❲THIN SPACE❳ ❲HAIR SPACE❳ ❲ZERO WIDTH SPACE❳ ❲NARROW NO-BREAK SPACE❳ ❲MEDIUM MATHEMATICAL SPACE❳ ❲SYMBOL FOR BACKSPACE❳ ❲SYMBOL FOR SPACE❳ ❲IDEOGRAPHIC SPACE❳ ❲IDEOGRAPHIC HALF FILL SPACE❳ ❲\\ud836!!SURROGATE❳ ❲\\ud836!!SURROGATE❳ ! ␀ ␁ ␂ ␃ ␄ ␅ ␆ ␇ ␈ ␉ ⏎ ␋ ␌ ␍ ␎ ␏ ␐ ␑ ␒ ␓ ␔ ␕ ␖ ␗ ␘ ␙ ␚ ␛ ␜ ␝ ␞ ␟ ␡")
        }
    }

    @Test
    @Disabled
    internal fun `should show progress bar`() {
        Exec.Sync.execCommand(command = "sh",
            arguments = arrayOf("progressbar.sh"),
            workingDirectory = Path.of("").toAbsolutePath().resolve("src/main/resources"),
            outputProcessor = { if (it.type == OUT) println(System.out) })
        (0..100).forEach {
            TermUi.echo("\u0013" + "X".repeat(it % 10))
            Thread.sleep(50)
        }
    }
}
