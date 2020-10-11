package com.bkahlert.koodies.string

/**
 * Returns this [CharSequence] truncated to [length] and if necessary padded from the start.
 */
fun CharSequence.padStartFixedLength(
    length: Int = 15,
    strategy: TruncationStrategy = TruncationStrategy.END,
    marker: String = "…",
    padChar: Char = ' ',
): String =
    strategy.truncate(toString(), length, marker).padStart(length, padChar)