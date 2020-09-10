package com.imgcstmzr.patch.ini

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class CompositeRegexElement(private val initialInput: String) {
    private val optionalProperties: MutableMap<Property<*>, Lazy<RegexElement?>> = linkedMapOf()

    fun <T : RegexElement> optionalElement(elementFactory: (String) -> T?): Property<T> {
        val property = Property<T>()
        optionalProperties[property] = lazy {
            runCatching {
                val element: T? = elementFactory(initialInput)
                element?.toString() // HACK: better evaluate element explicitly and not as a side effect
                element
            }.getOrNull()
        }
        return property
    }

    inner class Property<out T : RegexElement>() : ReadOnlyProperty<CompositeRegexElement, T?> {
        override fun getValue(thisRef: CompositeRegexElement, property: KProperty<*>): T? {
            val matchedElementContainer = optionalProperties[this]
            val matchedElement: RegexElement? = if (matchedElementContainer != null) matchedElementContainer.value
            else throw IllegalStateException("Expression 'optionalProperties[this]' must not be null")
            return matchedElement as T?
        }
    }

    override fun toString(): String = optionalProperties.values.joinToString("") { lazy: Lazy<RegexElement?> -> lazy.value?.toString() ?: "" }
}
