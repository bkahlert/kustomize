package com.imgcstmzr.cli

import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.writeText
import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.util.debug
import java.nio.file.Path


class KeyValueDocument(private val properties: MutableList<Pair<String, MutableList<String>>>) {

    constructor(content: String) : this(content.split(PROPERTY_SEPARATOR).map { property ->
        val split: List<String> = property.split(KEY_VALUE_SEPARATOR)
        val key = split[0]
        val values: MutableList<String> = if (split.size > 1) split[1].split(VALUE_SEPARATOR).map { it.trim() }.toMutableList() else mutableListOf()
        key to values
    }.toMutableList())

    constructor(path: Path) : this(path.readText())

    fun findProperty(key: String): Pair<String, MutableList<String>>? = properties.find { (k, _) -> key == k }

    fun containsValue(key: String, value: String): Boolean =
        properties.find { (k, v) -> key == k && v.contains(value) }?.toList()?.isNotEmpty() ?: false

    fun addValue(key: String, value: String) {
        val foundProperties: Pair<String, MutableList<String>>? = findProperty(key)
        if (foundProperties == null) properties.add(key to mutableListOf(value))
        else {
            if (!foundProperties.second.contains(value)) {
                val second: MutableList<String> = foundProperties.second
                second.add(value)
            } else {
                TermUi.debug("$foundProperties already contains $value")
            }
        }
    }

    fun save(path: Path) {
        path.writeText(toString())
    }

    override fun toString(): String {
        return properties.joinToString(" ") {
            val key = it.first
            val value = it.second.joinToString(",")
            "$key=$value"
        }
    }

    companion object {
        private val PROPERTY_SEPARATOR = Regex("\\s+")
        private val KEY_VALUE_SEPARATOR = Regex("\\s*=\\s*")
        private const val VALUE_SEPARATOR = ","
    }
}
