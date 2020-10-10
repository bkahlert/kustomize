package com.bkahlert.koodies.collections

import com.bkahlert.koodies.number.`%+`
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Returns the same list with two differences:
 * 1) Negative indices are supported and start from the end of this list (e.g. `this[-1]` returns the last element, `this[-2]` returns the second, and so on).
 * 2) Modulus operation is applied. E.g. `listOf("a","b","c").withNegativeIndices(4)` returns `a`. `this[-4]` would return `c`.
 */
inline fun <reified T> List<T>.withNegativeIndices(): List<T> {
    return object : List<T> by this {
        override fun get(index: Int): T = this@withNegativeIndices[index `%+` size]
    }
}

/**
 * Returns the same list providing function with two differences:
 * 1) Negative indices are supported and start from the end of this list (e.g. `this[-1]` returns the last element, `this[-2]` returns the second, and so on).
 * 2) Modulus operation is applied. E.g. `listOf("a","b","c").withNegativeIndices(4)` returns `a`. `this[-4]` would return `c`.
 */
inline fun <reified T> (() -> List<T>).withNegativeIndices(): () -> List<T> = { this().withNegativeIndices() }

/**
 * Returns a [ReadOnlyProperty] that delegates all calls to [listProvider] unmodified with one exception:
 * The returned [List] is wrapped to allow negative indices:
 * 1) Negative indices are supported and start from the end of the list (e.g. `this[-1]` returns the last element, `this[-2]` returns the second, and so on).
 * 2) Modulus operation is applied. E.g. `listOf("a","b","c").withNegativeIndices(4)` returns `a`. `this[-4]` would return `c`.
 */
inline fun <reified T, reified V> withNegativeIndices(noinline listProvider: () -> List<V>): ReadOnlyProperty<T, List<V>> =
    ReadOnlyProperty { thisRef: T, property: KProperty<*> -> listProvider().withNegativeIndices() }
