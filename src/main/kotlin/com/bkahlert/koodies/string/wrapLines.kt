package com.bkahlert.koodies.string

/**
 * Returns a string consisting of lines of which none is longer than [maxLineLength].
 */
fun CharSequence.wrapLines(maxLineLength: Int): CharSequence =
    linesOfLength(maxLineLength).joinLinesToString("") { "$it" }
