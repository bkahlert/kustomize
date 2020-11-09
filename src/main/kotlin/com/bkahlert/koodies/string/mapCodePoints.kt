package com.bkahlert.koodies.string

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each [CodePoint] of this string.
 */
fun <R> String.mapCodePoints(transform: (CodePoint) -> R): List<R> =
    codePointSequence().map(transform).toList()

