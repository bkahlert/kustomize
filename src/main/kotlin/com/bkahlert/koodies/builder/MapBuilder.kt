package com.bkahlert.koodies.builder

/**
 * Convenience type to easier use [buildMap] accepts.
 */
typealias MapBuilderInit<K, V> = MapBuilder<K, V>.() -> Unit

class MapBuilder<K, in V>(private val map: MutableMap<K, V>) {
    companion object {
        inline fun <reified K, reified V> build(init: MapBuilderInit<K, V>): Map<K, V> =
            linkedMapOf<K, V>().also { MapBuilder(it).apply(init) }
    }

    infix fun K.to(value: V) {
        map[this] = value
    }

    operator fun Map<K, V>.unaryPlus() {
        map.putAll(this)
    }

    operator fun Map<K, V>.unaryMinus() {
        forEach { (key, value) -> map.remove(key, value) }
    }
}

/**
 * Using `this` [MapBuilderInit] builds a map of [K] and [V].
 */
fun <K, V> MapBuilderInit<K, V>.buildMap(): Map<K, V> =
    linkedMapOf<K, V>().also { MapBuilder(it).this() }

/**
 * Using `this` [MapBuilderInit] builds a map of [K] and [V].
 *
 * As as side effect the result is added to [target].
 */
fun <K, V> MapBuilderInit<K, V>.buildMapTo(target: MutableMap<in K, in V>): Map<K, V> =
    buildMap().also { target.putAll(it) }

/**
 * Using `this` [MapBuilderInit] builds a map of [K] and [V]
 * and applies [transform] to the result.
 */
fun <K, V, T> MapBuilderInit<K, V>.buildMap(transform: Map.Entry<K, V>.() -> T): List<T> =
    buildMap().entries.map { it.transform() }

/**
 * Using `this` [MapBuilderInit] builds a map of [K] and [V]
 * and applies [transform] to the result.
 *
 * As as side effect the transformed result is added to [target].
 */
fun <K, V, T> MapBuilderInit<K, V>.buildMapTo(target: MutableCollection<in T>, transform: Map.Entry<K, V>.() -> T): List<T> =
    buildMap(transform).also { target.addAll(it) }
