package com.bkahlert.koodies.string

/**
 * Repeats this [Char] [count] times.
 */
fun Char.repeat(count: Int): String = String(CharArray(count) { this })

/**
 * Repeats this [CharSequence] [count] times.
 */
fun <T : CharSequence> T.repeat(count: Int): String = StringBuilder().also { repeat(count) { _ -> it.append(this@repeat) } }.toString()
