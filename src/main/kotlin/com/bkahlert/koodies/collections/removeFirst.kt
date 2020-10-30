package com.bkahlert.koodies.collections

/**
 * Removes the [n] elements from this mutable list and returns those removed elements,
 * or throws [IllegalArgumentException] if this list contains fewer elements.
 */
fun <T> MutableList<T>.removeFirst(n: Int): List<T> {
    require(n <= size) { "Attempted to remove first $n elements although only $size were present." }
    return (0 until n).map { removeFirst() }
}
