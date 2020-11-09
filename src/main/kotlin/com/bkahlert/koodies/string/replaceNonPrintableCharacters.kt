package com.bkahlert.koodies.string

import com.bkahlert.koodies.number.toHexString
import com.bkahlert.koodies.string.Unicode.replacementSymbol
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.unit


/**
 * Replaces control (e.g. [Unicode.escape], surrogate (e.g. `\ubd00`) and whitespace (e.g. [Unicode.lineFeed]) characters
 * with a visual representation or, if unavailable, with their written Unicode name.
 */
fun String.replaceNonPrintableCharacters(): String {
    return mapCodePoints { codePoint ->
        val prefix = if (codePoint.string in LineSeparators) "⏎" else ""
        val suffix = if (codePoint.char in Unicode.controlCharacters.values) "ꜝ" else ""
        prefix + when {
            codePoint.char == ' ' -> " "
            codePoint.replacementSymbol != null -> codePoint.replacementSymbol.toString()
            codePoint.string in LineSeparators -> "⏎"
            codePoint.isHighSurrogate -> codePoint.codePoint.toHexString(pad = true) + "▌﹍"
            codePoint.isLowSurrogate -> "﹍▐" + codePoint.codePoint.toHexString(pad = true)
            codePoint.isWhitespace || codePoint.char in Unicode.whitespaces -> codePoint.unicodeName.unit()
            else -> codePoint.string
        } + suffix
    }.joinToString("")
}
