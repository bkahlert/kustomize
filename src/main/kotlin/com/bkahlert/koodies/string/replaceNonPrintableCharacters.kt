package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.Unicode.replacementSymbol
import com.bkahlert.koodies.string.Unicode.unicodeName

private val boxDrawings by lazy { Regex.escape(Unicode.boxDrawings.joinToString("")) }
private val specialCharacterPattern by lazy { Regex("[^\\p{Print}\\p{IsPunctuation}$boxDrawings]") }

/**
 * Replaces all special/non-printable characters, that is, all characters but \x20 (space) to \x7E (tilde) with their Unicode name.
 */
fun String.replaceNonPrintableCharacters(): String = this.replace(specialCharacterPattern) {
    val char = it.value.first()
    if (char.replacementSymbol != null) char.replacementSymbol.toString()
    else "❲" + char.unicodeName + "❳"
}
