package koodies.collections

import koodies.text.toLowerCase

fun <V> Map<String?, V>.caseIgnoringKeys(): Map<String?, V> {
    val transform: (String?) -> String? = { it?.toLowerCase() }
    val delegate = mapKeys { transform(it.key) }
    return object : Map<String?, V> by this {
        override fun containsKey(key: String?): Boolean = delegate.containsKey(transform(key))
        override fun get(key: String?): V? = delegate[transform(key)]
        override fun getOrDefault(key: String?, defaultValue: V): V = delegate.getOrDefault(transform(key), defaultValue)
        override val keys: Set<String?> = delegate.keys
        override val entries: Set<Map.Entry<String?, V>> = delegate.entries
        override fun toString(): String = entries.toString()
    }
}
