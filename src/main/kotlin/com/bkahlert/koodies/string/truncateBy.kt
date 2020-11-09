package com.bkahlert.koodies.string


/**
 * Truncates this char sequence by [numberOfWhitespaces] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
fun CharSequence.truncateBy(numberOfWhitespaces: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): String =
    toString().truncateBy(numberOfWhitespaces, startIndex, minWhitespaceLength)

/**
 * Truncates this string by [numberOfWhitespaces] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
fun String.truncateBy(numberOfWhitespaces: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): String =
    truncateTo(length - numberOfWhitespaces, startIndex, minWhitespaceLength).asString()
