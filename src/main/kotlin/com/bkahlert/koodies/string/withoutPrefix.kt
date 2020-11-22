package com.bkahlert.koodies.string

/**
 * If this char sequence starts with the given [prefix], returns a new char sequence
 * with the prefix removed. Otherwise, returns a new char sequence with the same characters.
 */
fun CharSequence.withoutPrefix(prefix: CharSequence, ignoreCase: Boolean = false): CharSequence {
    if (startsWith(prefix, ignoreCase = ignoreCase)) {
        return subSequence(prefix.length, length)
    }
    return subSequence(0, length)
}

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
fun String.withoutPrefix(prefix: CharSequence, ignoreCase: Boolean = false): String {
    if (startsWith(prefix, ignoreCase = ignoreCase)) {
        return substring(prefix.length)
    }
    return this
}
