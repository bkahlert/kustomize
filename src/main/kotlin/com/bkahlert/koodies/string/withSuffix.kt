package com.bkahlert.koodies.string

/**
 * Returns this [CharSequence] with the [suffix] appended if it is not already there.
 */
fun CharSequence.withSuffix(suffix: String): String = if (endsWith(suffix)) this.toString() else this.toString() + suffix
