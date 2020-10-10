@file:Suppress("FunctionName")

package com.bkahlert.koodies.number

/** Calculates the positive remainder of dividing this value by the other value. */
fun Byte.prem(other: Byte): Int = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
infix fun Byte.`%+`(other: Byte): Int = prem(other)

/** Calculates the positive remainder of dividing this value by the other value. */
fun Short.prem(other: Short): Int = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
infix fun Short.`%+`(other: Short): Int = prem(other)

/** Calculates the positive remainder of dividing this value by the other value. */
fun Int.prem(other: Int): Int = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
infix fun Int.`%+`(other: Int): Int = prem(other)

/** Calculates the positive remainder of dividing this value by the other value. */
fun Long.prem(other: Long): Long = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
infix fun Long.`%+`(other: Long): Long = prem(other)

/** Calculates the positive remainder of dividing this value by the other value. */
fun Float.prem(other: Float): Float = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
infix fun Float.`%+`(other: Float): Float = prem(other)

/** Calculates the positive remainder of dividing this value by the other value. */
fun Double.prem(other: Double): Double = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
infix fun Double.`%+`(other: Double): Double = prem(other)
