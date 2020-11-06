package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLength
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences

/**
 * A char sequence which is [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) aware
 * and which does not break any sequence.
 *
 * The behaviour is as follows:
 * - escape sequences have length 0, that is, an [AnsiString] has the same length as its [String] counterpart
 * - [get] returns the unformatted char at the specified index
 * - [subSequence] returns the same char sequence as an unformatted [String] would do—but with the formatting ANSI escape sequences intact.
 * the sub sequence. Also escape sequences are ignored from [length].
 */
class AnsiString(val charSequence: CharSequence) : CharSequence {
    val unformatted = charSequence.removeEscapeSequences()

    /**
     * Returns the logical length of this string. That is, the same length as the unformatted [String] would return.
     */
    override val length: Int
        get() = unformatted.length

    /**
     * Returns the unformatted char at the specified [index].
     *
     * Due to the limitation of a [Char] to two byte no formatted [Char] can be returned.
     */
    override fun get(index: Int): Char = unformatted[index]

    /**
     * Returns the same char sequence as an unformatted [String.subSequence] would do.
     *
     * Sole difference: The formatting ANSI escape sequences are kept.
     * Eventually open sequences will be closes at the of the sub sequence.
     */
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (endIndex > ansiAwareLength()) throw IndexOutOfBoundsException(endIndex)

        val prefix = if (startIndex > 0) subSequence(0, startIndex) as AnsiSubstring else AnsiSubstring("", emptyList())
        val substring = StringBuilder(prefix.openingString)
        val codes = prefix.unclosedCodes.toMutableList()
        var read = prefix.toString().ansiAwareLength()
        var consumed = prefix.string.length
        while (read < endIndex && consumed < charSequence.length) {
            val match = AnsiCode.regex.find(charSequence, consumed)
            val range = match?.range
            if (range?.first == consumed) {
                val ansiCodeString: String = charSequence.substring(consumed, match.range.last + 1).also {
                    val currentCodes = AnsiCode.parseAnsiCode(match).toList()
                    codes.addAll(currentCodes)
                    consumed += it.length
                }
                substring.append(ansiCodeString)
            } else {
                val readAtMostTill: Int = (endIndex - read) + consumed
                val first: Int? = range?.first
                val ansiAhead = if (first != null) first < readAtMostTill else false
                val substring1 = charSequence.substring(consumed, if (ansiAhead) first!! else readAtMostTill)
                val ansiCodeFreeString = substring1.also {
                    read += it.length
                    consumed += it.length
                }
                substring.append(ansiCodeFreeString)
            }
        }
        val unclosedCodes = mutableListOf<Int>()
        codes.forEach { code ->
            val ansiCodes: List<AnsiCode> = AnsiCode.codeToAnsiCodeMappings[code] ?: emptyList()
            ansiCodes.forEach { ansiCode ->
                if (ansiCode.closeCode != code) {
                    unclosedCodes.addAll(ansiCode.openCodes)
                } else {
                    unclosedCodes.removeAll { ansiCode.openCodes.contains(it) }
                }
            }
        }
        return AnsiSubstring("$substring", unclosedCodes).let { x ->
            object : AnsiSubstring("$substring", unclosedCodes), CharSequence by "$x" {

            }
        }
    }

    companion object {

    }
}

open class AnsiSubstring(val string: String, val unclosedCodes: List<Int>) {
    val closingCodes = unclosedCodes.flatMap { openCode -> AnsiCode.codeToAnsiCodeMappings[openCode]?.map { ansiCode -> ansiCode.closeCode } ?: emptyList() }
    val closingString = com.github.ajalt.mordant.AnsiCode(closingCodes, 0)(AnsiCode.splitCodeMarker).split(AnsiCode.splitCodeMarker)[0]
    val openingString = com.github.ajalt.mordant.AnsiCode(unclosedCodes, 0)(AnsiCode.splitCodeMarker).split(AnsiCode.splitCodeMarker)[0]
    override fun toString(): String {
        val x = com.github.ajalt.mordant.AnsiCode(closingCodes, 0)(AnsiCode.splitCodeMarker)
        val y = x.split(AnsiCode.splitCodeMarker)[0]
        return string + y
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnsiSubstring

        if (string != other.string) return false
        if (unclosedCodes != other.unclosedCodes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = string.hashCode()
        result = 31 * result + unclosedCodes.hashCode()
        return result
    }

}
