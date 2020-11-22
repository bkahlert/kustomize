package com.bkahlert.koodies.string

/**
 * If this char sequence ends with the given [suffix], returns a new char sequence
 * with the suffix removed. Otherwise, returns a new char sequence with the same characters.
 */
fun CharSequence.withoutSuffix(suffix: CharSequence, ignoreCase: Boolean = false): CharSequence {
    if (endsWith(suffix, ignoreCase = ignoreCase)) {
        return subSequence(0, length - suffix.length)
    }
    return subSequence(0, length)
}

/**
 * If this string ends with the given [suffix], returns a copy of this string
 * with the suffix removed. Otherwise, returns this string.
 */
fun String.withoutSuffix(suffix: CharSequence, ignoreCase: Boolean = false): String {
    if (endsWith(suffix, ignoreCase = ignoreCase)) {
        return substring(0, length - suffix.length)
    }
    return this
}
