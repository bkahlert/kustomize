package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.terminal.removeEscapeSequences
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Centers this collection of strings by adding the needed amount of whitespaces from the left (and right)
 * of each line.
 *
 * For example:
 * ```
 * foo
 *   bar baz
 * ```
 * becomes
 * ```
 *   foo
 * bar baz
 * ```
 */
fun <T : CharSequence> Collection<T>.center(whitespace: Char = '\u00A0'): List<String> {
    return map { it.trim() }.let { trimmed ->
        trimmed.maxOfOrNull { it.removeEscapeSequences().length }?.let { maxLength ->
            trimmed.map { line ->
                val missing: Double = (maxLength - line.removeEscapeSequences().length) / 2.0
                whitespace.repeat(floor(missing).toInt()) + line + whitespace.repeat(ceil(missing).toInt())
            }.toList()
        } ?: emptyList()
    }
}


/**
 * Centers this collection of strings by adding the needed amount of whitespaces from the left (and right)
 * of each line.
 *
 * For example:
 * ```
 * foo
 *   bar baz
 * ```
 * becomes
 * ```
 *   foo
 * bar baz
 * ```
 */
fun <T : CharSequence> T.center(whitespace: Char = '\u00A0'): String = lines().center(whitespace).joinToString("\n")
