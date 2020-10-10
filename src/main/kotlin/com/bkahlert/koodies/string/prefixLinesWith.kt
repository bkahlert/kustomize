package com.bkahlert.koodies.string

/**
 * Returns the [String] of what all lines of text are prefixed with the given [prefix].
 */
fun String.prefixLinesWith(prefix: String): String = mapLines { prefix + it }
