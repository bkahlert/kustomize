package com.bkahlert.koodies.regex

/**
 * Returns a sequence of all occurrences of this regular expression within
 * the [input] string, beginning at the specified [startIndex].
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or
 *         greater than the length of the [input] char sequence.
 */
fun Regex.sequenceOfAllMatches(input: CharSequence, startIndex: Int = 0): Sequence<String> =
    findAll(input, startIndex).map { it.value }
