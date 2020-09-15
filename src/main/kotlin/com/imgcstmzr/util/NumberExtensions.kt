package com.imgcstmzr.util

val Number.isEven get():Boolean = toInt() % 2 == 0

/**
 * Calls the [function] [this] times.
 *
 * Examples:
 * ```
 * 3 * { i -> println(i) }
 * ```
 *
 * ```
 * 8 times { _ -> action() }
 * ```
 */
inline infix operator fun <reified T : Number> T.times(function: (Int) -> Unit) {
    (0..this.toInt()).forEach { i -> function(i) }
}

fun Int.positiveRemainder(divisor: Int): Int = (this % divisor).let { if (it < 0) it + divisor else it }

infix fun Int.`%+`(divisor: Int): Int = positiveRemainder(divisor)


