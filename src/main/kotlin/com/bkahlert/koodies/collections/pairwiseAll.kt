package com.bkahlert.koodies.collections

/**
 * Returns `true` if the number of [predicates] is the same and each element matches its corresponding predicate.
 */
fun <T> List<T>.pairwiseAll(vararg predicates: (T) -> Boolean): Boolean =
    size == predicates.size && (predicates.indices).all { i -> this[i].let(predicates[i]) }


/**
 * Returns `true` if the number of [predicates] is the same and each element matches its corresponding predicate.
 */
fun <T> Array<T>.pairwiseAll(vararg predicates: (T) -> Boolean): Boolean =
    size == predicates.size && (predicates.indices).all { i -> this[i].let(predicates[i]) }
