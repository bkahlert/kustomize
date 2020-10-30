package com.bkahlert.koodies.string

/**
 * Returns `true` if this char sequence contains any of the specified [others] as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
fun <T : CharSequence> CharSequence.containsAny(others: Iterable<T>, ignoreCase: Boolean = false) =
    others.any { contains(it, ignoreCase = ignoreCase) }
