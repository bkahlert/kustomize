package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.LineSeparators.trailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator

/**
 * Maps each line of this strings the [String] using [transform].
 *
 * If this strings consists of but a single line this line is mapped.
 *
 * If this string has a trailing line that trailing line is left unchanged.
 */
fun String.mapLines(ignoreTrailingSeparator: Boolean = true, transform: (String) -> String): String {
    val trailingLineBreak = trailingLineSeparator
    val prefixedLines = withoutTrailingLineSeparator.lines().joinToString("\n") { line ->
        transform(line)
    }
    return prefixedLines + (trailingLineBreak?.let {
        trailingLineBreak + (if (!ignoreTrailingSeparator) transform("") else "")
    } ?: "")
}
