package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.closingControlSequence
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.controlSequence
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.parseAnsiCodesAsSequence
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.unclosedCodes

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
open class AnsiString private constructor(private val tokens: List<Pair<CharSequence, Int>>) : CharSequence {
    constructor(charSequence: CharSequence) : this("$charSequence".tokenize())

    companion object {
        val EMPTY = AnsiString("")

        fun <T : CharSequence> T.asAnsiString(): AnsiString = if (this.isEmpty()) EMPTY else AnsiString(this)

        fun String.tokenize(): List<Pair<CharSequence, Int>> {
            val tokens = mutableListOf<Pair<CharSequence, Int>>()
            val codes = mutableListOf<Int>()
            var consumed = 0
            while (consumed < length) {
                val match = AnsiCode.ansiCodeRegex.find(this, consumed)
                val range = match?.range
                if (range?.first == consumed) {
                    val ansiCodeString = this.subSequence(consumed, match.range.last + 1).also {
                        val currentCodes = AnsiCode.parseAnsiCode(match).toList()
                        codes.addAll(currentCodes)
                        consumed += it.length
                    }
                    tokens.add(ansiCodeString to 0)
                } else {
                    val first: Int? = range?.first
                    val ansiAhead = if (first != null) first < length else false
                    val substring1 = this.subSequence(consumed, if (ansiAhead) first!! else length)
                    val ansiCodeFreeString = substring1.also {
                        consumed += it.length
                    }
                    tokens.add(ansiCodeFreeString to ansiCodeFreeString.length)
                }
            }
            return tokens
        }

        val List<Pair<CharSequence, Int>>.length get():Int = sumBy { it.second }

        fun List<Pair<CharSequence, Int>>.subSequence(endIndex: Int): Pair<String, List<Int>> {
            if (endIndex == 0) return "" to emptyList()
            if (endIndex > length) throw IndexOutOfBoundsException(endIndex)
            var read = 0
            val codes = mutableListOf<Int>()
            val sb = StringBuilder()
            forEach { token ->
                val needed = endIndex - read
                if (needed > 0 && token.second == 0) {
                    sb.append(token.first)
                    codes.addAll(token.first.parseAnsiCodesAsSequence())
                } else {
                    if (needed <= token.second) {
                        sb.append(token.first.subSequence(0, needed))
                        return@subSequence "$sb" + closingControlSequence(codes) to unclosedCodes(codes)
                    }
                    sb.append(token.first)
                    read += token.second
                }
            }
            error("must not happen")
        }

        fun List<Pair<CharSequence, Int>>.subSequence(startIndex: Int, endIndex: Int): String {
            if (startIndex > 0) {
                subSequence(startIndex).let { (prefix, unclosedCodes) ->
                    val (full, _) = subSequence(endIndex)
                    val controlSequence = controlSequence(unclosedCodes)
                    val startIndex1 = prefix.length - closingControlSequence(unclosedCodes).length
                    val endIndex1 = full.length
                    val x =
                        controlSequence + full.subSequence(startIndex1, endIndex1)
                    return x
                }
            } else {
                return subSequence(endIndex).first
            }
        }

        fun List<Pair<CharSequence, Int>>.getChar(index: Int): Char {
            if (index > length) throw IndexOutOfBoundsException(index)
            var read = 0
            forEach { token ->
                val needed = index - read
                if (token.second >= 0) {
                    if (needed <= token.second) {
                        return token.first[needed]
                    }
                    read += token.second
                }
            }
            error("must not happen")
        }

        fun List<Pair<CharSequence, Int>>.render(ansi: Boolean = true) =
            if (ansi) subSequence(0, length)
            else filter { it.second != 0 }.joinToString("") { it.first }
    }

    val string: String = tokens.render(ansi = true)

    /**
     * Contains this [string] with all [ANSI] escape sequences removed.
     */
    @Suppress("SpellCheckingInspection")
    val unformatted = tokens.render(ansi = false)

    /**
     * Returns the logical length of this string. That is, the same length as the unformatted [String] would return.
     */
    override val length: Int get() = unformatted.length

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
    override fun subSequence(startIndex: Int, endIndex: Int): AnsiString =
        tokens.subSequence(startIndex, endIndex).asAnsiString()

    fun toString(withoutAnsi: Boolean = false): String =
        if (withoutAnsi) unformatted
        else string

    override fun toString(): String = toString(false)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnsiString

        if (string != other.string) return false

        return true
    }

    override fun hashCode(): Int = string.hashCode()
}
