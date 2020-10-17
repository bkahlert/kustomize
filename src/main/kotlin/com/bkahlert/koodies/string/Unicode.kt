package com.bkahlert.koodies.string

object Unicode {

    /**
     * Returns the [CodePoint] with the specified index.
     */
    operator fun get(codePoint: Int): CodePoint = CodePoint(codePoint)

    /**
     * [CARRIAGE RETURN (CR)](https://codepoints.net/U+000D)
     */
    const val carriageReturn = "\r"

    /**
     * [LINE FEED (LF)](https://codepoints.net/U+000A)
     */
    const val lineFeed = "\n"

    /**
     * [LINE SEPARATOR](https://codepoints.net/U+2028)
     */
    const val lineSeparator = "\u2028"

    /**
     * [PARAGRAPH SEPARATOR](https://codepoints.net/U+2029)
     */
    const val paragraphSeparator = "\u2029"

    /**
     * [NEXT LINE (NEL)](https://codepoints.net/U+0085)
     */
    const val nextLine = "\u0085"

    /**
     * [RIGHT-TO-LEFT MARK](https://codepoints.net/U+200F)
     */
    const val rightToLeftMark = '\u200F'

    /**
     * [REPLACEMENT CHARACTER](https://codepoints.net/U+FFFD) �
     */
    const val replacementCharacter = '\uFFFD'

    object DivinationSymbols {
        const val tetragramForEnlargment = "\uD834\uDF33" // 𝌳
        const val tetragramForPurety = "\uD834\uDF2A" // 𝌪
    }


    @Suppress("SpellCheckingInspection")
    var whitespaces: List<Char> = listOf(
        '\u0020', // SPACE: Depends on font, typically 1/4 em, often adjusted
        '\u00A0', // NO-BREAK SPACE: As a space, but often not adjusted
        '\u1680', // OGHAM SPACE MARK: Unspecified; usually not really a space but a dash
        '\u180E', // MONGOLIAN VOWEL SEPARATOR: 0
        '\u2000', // EN QUAD: 1 en (= 1/2 em)
        '\u2001', // EM QUAD: 1 em (nominally, the height of the font)
        '\u2002', // EN SPACE (nut): 1 en (= 1/2 em)
        '\u2003', // EM SPACE (mutton): 1 em
        '\u2004', // THREE-PER-EM SPACE (thick space): 1/3 em
        '\u2005', // FOUR-PER-EM SPACE (mid space): 1/4 em
        '\u2006', // SIX-PER-EM SPACE: 1/6 em
        '\u2007', // FIGURE SPACE	fo: “Tabular width”, the width of digits
        '\u2008', // PUNCTUATION SPACE: The width of a period “.”
        '\u2009', // THIN SPACE: 1/5 em (or sometimes 1/6 em)
        '\u200A', // HAIR SPACE: Narrower than THIN SPACE
        '\u200B', // ZERO WIDTH SPACE: 0
        '\u202F', // NARROW NO-BREAK SPACE	fo: Narrower than NO-BREAK SPACE (or SPACE), “typically the width of a thin space or a mid space”
        '\u205F', // MEDIUM MATHEMATICAL SPACE: 4/18 em
        '\u3000', // IDEOGRAPHIC SPACE: The width of ideographic (CJK) characters.
        '\uFEFF', // ZERO WIDTH NO-BREAK SPACE: 0
    )

    var boxDrawings = ('\u2500'..'\u257F').toList()
}
