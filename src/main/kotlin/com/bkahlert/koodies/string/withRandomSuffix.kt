package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.Random.alphanumericCharacters

/**
 * Returns this [CharSequence] with a random suffix of one dash and four alpha-numeric characters.
 */
fun CharSequence.withRandomSuffix(): String {
    if (lastMatcher.matches(this)) return this.toString()
    return "$this-${String.random(length = suffixLength, allowedCharacters = alphanumericCharacters)}"
}

private val suffixLength = 4
private val lastMatcher: Regex = Regex(".*-[0-9a-zA-Z]{$suffixLength}\$")

