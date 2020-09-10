package com.imgcstmzr.patch.ini

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class RegexElement(private val initialInput: String, private val matchCompleteLine: Boolean = false) {
    private val properties: MutableList<Property> = mutableListOf()
    private lateinit var parser: RegexParser
    val regex: Regex
        get() {
            parseIfNecessary()
            return parser.regex
        }
    private lateinit var parsed: ParsedElement

    private fun parseIfNecessary() {
        if (!::parser.isInitialized) parser = RegexParser(properties.map { it.name to it.pattern }, matchCompleteLine)
        if (!::parsed.isInitialized) parsed = parser.parseSingle(initialInput)
    }

    fun regex(pattern: String): Property = Property(pattern).also { properties.add(it) }
    class Property(val pattern: String) : ReadWriteProperty<RegexElement, String> {
        lateinit var name: String

        /**
         * Used to find out as soon as possible statically what the property name is, this [Property] is assigned to.
         * Needed in order to have all declared properties ready when the first one is actually accessed (which triggers [parseIfNecessary] which builds the actual [RegexParser].
         */
        operator fun provideDelegate(thisRef: RegexElement, prop: KProperty<*>): Property {
            name = prop.name
            return this
        }

        override fun getValue(thisRef: RegexElement, property: KProperty<*>): String {
            thisRef.parseIfNecessary()
            return thisRef.parsed[property.name] ?: throw IllegalStateException("This must never happen")
        }

        override fun setValue(thisRef: RegexElement, property: KProperty<*>, value: String) {
            thisRef.parseIfNecessary()
            val parsed = thisRef.parsed
            thisRef.parsed = thisRef.parser.replace(parsed, property.name to value)
        }
    }

    override fun toString(): String {
        parseIfNecessary()
        return parsed.toString()
    }
}
