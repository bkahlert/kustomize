package com.bkahlert.koodies.terminal

import com.bkahlert.koodies.terminal.ASCII.Masking.blend
import com.imgcstmzr.util.replaceNonPrintableCharacters
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class ASCIITest {

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
}

