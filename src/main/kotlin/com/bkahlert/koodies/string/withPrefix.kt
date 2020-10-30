package com.bkahlert.koodies.string

/**
 * Returns this [CharSequence] with the [prefix] prepended if it is not already there.
 */
fun CharSequence.withPrefix(prefix: String): String = if (startsWith(prefix)) asString() else prefix + asString()
