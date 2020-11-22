package com.bkahlert.koodies

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class SpyableProperty<V>(private val spiedProperty: () -> V) : ReadOnlyProperty<Any?, () -> V> {

    protected open fun afterAccess(returnValue: V): Unit {}

    override fun getValue(thisRef: Any?, property: KProperty<*>): () -> V {
        return {
            val returnValue = spiedProperty.invoke()
            afterAccess(returnValue)
            returnValue
        }
    }
}

inline fun <T> spyable(
    noinline spiedProperty: () -> T,
    crossinline afterAccess: (returnValue: T) -> Unit,
):
    ReadOnlyProperty<Any?, () -> T> =
    object : SpyableProperty<T>(spiedProperty) {
        override fun afterAccess(returnValue: T): Unit = afterAccess(returnValue)
    }
