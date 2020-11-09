package com.bkahlert.koodies.collections

/**
 * Returns a list of values built from the elements of `this` collection and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the longest collection—filling missing values with [default].
 */
inline fun <T, R, V> Iterable<T>.zipWithDefault(other: Iterable<R>, default: Pair<T, R>, transform: (a: T, b: R) -> V): List<V> {
    val first = iterator()
    val second = other.iterator()
    val list = ArrayList<V>(maxOf(collectionSizeOrDefault(10), other.collectionSizeOrDefault(10)))
    while (first.hasNext() || second.hasNext()) {
        list.add(transform(if (first.hasNext()) first.next() else default.first, if (second.hasNext()) second.next() else default.second))
    }
    return list
}

/**
 * Returns a sequence of values built from the elements of `this` sequence and the [other] sequence with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The resulting sequence ends as soon as the longest input sequence ends—filling missing values with [default].
 *
 * The operation is _intermediate_ and _stateless_.
 */
fun <T, R, V> Sequence<T>.zipWithDefault(other: Sequence<R>, default: Pair<T, R>, transform: (a: T, b: R) -> V): Sequence<V> {
    return MergingSequenceWithDefault(this, other, default, transform)
}

fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int = if (this is Collection<*>) this.size else default
