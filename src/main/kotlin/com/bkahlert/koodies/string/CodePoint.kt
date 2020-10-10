package com.bkahlert.koodies.string

/**
 * Representation of a Unicode code point.
 *
 * Note: As not every Unicode code point forms a proper latter so does not every letter consist of a single code point.
 * If that is what you are looking for, use [Grapheme].
 */
inline class CodePoint(val codePoint: Int) {
    constructor(charSequence: CharSequence) : this(charSequence.toString()
        .also { require(isCodePoint(charSequence)) { "$it does not represent a single Unicode code point" } }
        .codePointAt(0))

    constructor(chars: CharArray) : this(String(chars))

    /**
     * Contains the character pointed to and represented by a [CharArray].
     */
    val chars: CharArray get() = Character.toChars(codePoint)

    /**
     * Contains the character pointed to and represented by a [String].
     */
    val string: String get() = Character.toString(codePoint)

    companion object {
        /**
         * `true` if these [Char] instances represent a *single* Unicode character.
         */
        fun isCodePoint(chars: CharSequence) = chars.let {
            val codePointCount = it.codePoints().unordered().limit(2).count()
            codePointCount == 1L
        }

        fun isCodePoint(chars: CharArray) = isCodePoint(String(chars))
        fun count(string: CharSequence): Long = string.codePoints().count()
        fun count(string: String): Long = string.codePoints().count()
    }
}
