package com.bkahlert.koodies.string

/**
 * Calculates the maximum length of overlapping regions between this
 * and the [other] char sequence.
 */
fun CharSequence.overlap(other: CharSequence): Int {
    var maxOverlap = other.length
    while (!regionMatches(length - maxOverlap, other, 0, maxOverlap)) {
        maxOverlap--
    }
    return maxOverlap
}
