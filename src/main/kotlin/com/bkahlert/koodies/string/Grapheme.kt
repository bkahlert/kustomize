package com.bkahlert.koodies.string

import java.text.BreakIterator
import kotlin.streams.asSequence

/**
 * Representation of a letter as perceived by a user.
 *
 * Graphemes are actually the smallest distinctive unit in a writing system
 * and consist of 1 or more instances of [CodePoint].
 */
inline class Grapheme(val codePoints: List<CodePoint>) {
    constructor(charSequence: CharSequence) : this(charSequence.codePoints().asSequence().map { CodePoint(it) }.toList()
        .also { require(count(charSequence) == 1) { "$it does not represent a single grapheme" } })

    /**
     * Contains the character pointed to and represented by a [String].
     */
    val asString: String get() = codePoints.joinToString("") { it.string }

    override fun toString(): String = asString

    companion object {
        /**
         * `true` if these [Char] instances represent a *single* grapheme.
         */
        fun isGrapheme(chars: CharSequence) = chars.let {
            val codePointCount = it.codePoints().unordered().limit(2).count()
            codePointCount == 1L
        }

        fun count(string: CharSequence): Int = count(string.toString())
        fun count(string: String): Int {
            val breakIterator: BreakIterator = BreakIterator.getCharacterInstance().also { it.setText(string) }
            var count = 0
            while (breakIterator.next() != BreakIterator.DONE) count++
            return count
        }

        fun <T : CharSequence> T.getGrapheme(index: Int): String {
            val boundary: BreakIterator = BreakIterator.getCharacterInstance().also { it.setText(this.toString()) }
            val end: Int = boundary.following(index)
            val start: Int = boundary.previous()
            return substring(start, end)
        }

        fun <T : CharSequence> T.getGraphemeCount(): Int = count(this)
    }
}
