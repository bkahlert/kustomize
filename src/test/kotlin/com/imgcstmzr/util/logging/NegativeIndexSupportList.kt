package com.imgcstmzr.util.logging

import com.imgcstmzr.util.`%+`
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class NegativeIndexSupportList<in K, out T>(private val listProvider: () -> List<T>) : ReadOnlyProperty<K, List<T>> {
    override fun getValue(thisRef: K, property: KProperty<*>): List<T> {
        return listProvider().let {
            object : List<T> by it {
                override fun get(index: Int): T = it[index `%+` size]
            }
        }
    }

    companion object {
        inline fun <reified K, reified T> negativeIndexSupport(noinline listProvider: () -> List<T>): NegativeIndexSupportList<K, T> =
            NegativeIndexSupportList(listProvider)
    }
}
