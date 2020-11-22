package com.bkahlert.koodies.collections

/**
 * Returns the maximum of the elements contained in this iterable.
 *
 * Throws [NoSuchElementException] is no element is present.
 */
fun <T : Comparable<T>> Iterable<T>.maxOrThrow(): T {
    val iterator = iterator()
    if (!iterator.hasNext()) throw NoSuchElementException()
    var max = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (max < e) max = e
    }
    return max
}
