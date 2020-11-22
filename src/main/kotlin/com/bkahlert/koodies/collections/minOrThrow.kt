package com.bkahlert.koodies.collections

/**
 * Returns the minimum of the elements contained in this iterable.
 *
 * Throws [NoSuchElementException] is no element is present.
 */
fun <T : Comparable<T>> Iterable<T>.minOrThrow(): T {
    val iterator = iterator()
    if (!iterator.hasNext()) throw NoSuchElementException()
    var min = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (min > e) min = e
    }
    return min
}
