package com.bkahlert.koodies.string

/**
 * Returns the [String] truncated to [maxLength] characters including the [marker].
 */
fun CharSequence.truncate(maxLength: Int = 15, strategy: TruncationStrategy = TruncationStrategy.END, marker: String = "…"): CharSequence =
    strategy.truncate(this, maxLength, marker)
