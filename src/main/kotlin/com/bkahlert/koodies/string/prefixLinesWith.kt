package com.bkahlert.koodies.string

/**
 * Returns the [String] of what all lines of text are prefixed with the given [prefix].
 */
fun String.prefixLinesWith(ignoreTrailingSeparator: Boolean = true, prefix: String): String = mapLines(ignoreTrailingSeparator) { prefix + it }
