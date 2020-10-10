package com.bkahlert.koodies.string

/**
 * Returns the [String] truncated to [maxLength] characters including the [marker].
 */
fun String.truncate(maxLength: Int = 15, strategy: TruncationStrategy = TruncationStrategy.END, marker: String = "…"): String =
    strategy.truncate(this, maxLength, marker)
