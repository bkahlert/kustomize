package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.removeEscapeSequences
import kotlin.text.contains as containsRegex

/**
 * Returns if this char sequence contains the specified [other] [CharSequence] as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 * @param ignoreAnsiFormatting ANSI formatting / escapes are ignored by default. Use `false` consider escape codes as well
 */
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
operator fun <T : CharSequence> T.contains(
    other: CharSequence,
    ignoreCase: Boolean = false,
    ignoreAnsiFormatting: Boolean = false,
): Boolean =
    if (ignoreAnsiFormatting)
        removeEscapeSequences().containsRegex(other.removeEscapeSequences(), ignoreCase)
    else
        containsRegex(other, ignoreCase)
