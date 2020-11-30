package com.bkahlert.koodies.builder

class MapBuilder<K, V>(private val map: MutableMap<K, V>) {
    companion object {
        inline fun <reified K, reified V> build(init: MapBuilder<K, V>.() -> Unit): Map<K, V> =
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
