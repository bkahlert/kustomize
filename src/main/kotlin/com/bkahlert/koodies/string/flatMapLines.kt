package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.LineSeparators.trailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator

/**
 * Flat maps each line of this char sequence using [transform].
 *
 * If this char sequence consists of but a single line this line is mapped.
 *
 * If this char sequence has a trailing line that trailing line is left unchanged.
 */
fun <T : CharSequence> T.flatMapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> Iterable<T>): String {
    val trailingLineBreak = trailingLineSeparator
    val prefixedLines = withoutTrailingLineSeparator.lines().joinToString("\n") { line ->
        transform(line).joinToString("\n")
    }
    return prefixedLines + (trailingLineBreak?.let {
        trailingLineBreak + (if (!ignoreTrailingSeparator) transform("").joinToString("\n") else "")
    } ?: "")
}
