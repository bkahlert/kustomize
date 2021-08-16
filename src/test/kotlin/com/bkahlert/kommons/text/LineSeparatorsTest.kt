package com.bkahlert.kommons.text

import com.bkahlert.kommons.text.LineSeparators.lines
import strikt.api.Assertion.Builder

fun <T : CharSequence> Builder<T>.lines(
    keepDelimiters: Boolean = false,
): Builder<List<String>> = get("lines %s") { lines(keepDelimiters) }
