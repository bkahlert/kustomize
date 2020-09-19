@file:Suppress("ClassName")

package com.bkahlert.koodies.string

/**
 * Returns true if this char sequence matches the given SLF4J / Logback style [pattern], like `I {} you have to {}`.
 *
 * @sample MatchesKtTest.matching_single_line_string
 * @sample MatchesKtTest.matching_multi_line_string
 */
fun CharSequence.matches(pattern: String, placeholder: String = "{}"): Boolean =
    this.matches(Regex(pattern.split(placeholder).joinToString(".*") { Regex.escape(it) }))
