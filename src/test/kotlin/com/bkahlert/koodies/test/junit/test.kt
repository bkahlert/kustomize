package com.bkahlert.koodies.test.junit

import com.imgcstmzr.util.slf4jFormat
import org.junit.jupiter.api.DynamicTest
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

inline fun <reified T> Iterable<T>.test(testNamePattern: String? = null, crossinline executable: (T) -> Unit) = map { input ->
    val (fallbackPattern: String, args: Array<*>) = when (input) {
        is KFunction<*> -> "for property: {}" to arrayOf(input.name)
        is KProperty<*> -> "for property: {}" to arrayOf(input.name)
        is Triple<*, *, *> -> "for: {} to {} to {}" to arrayOf(input.first, input.second, input.third)
        is Pair<*, *> -> "for: {} to {}" to arrayOf(input.first, input.second)
        else -> "for: {}" to arrayOf(input)
    }
    DynamicTest.dynamicTest(slf4jFormat(testNamePattern ?: fallbackPattern, *args)) { executable(input) }
}


