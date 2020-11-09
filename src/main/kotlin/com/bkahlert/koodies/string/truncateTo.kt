package com.bkahlert.koodies.string

/**
 * Truncates this string to [maxLength] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
fun CharSequence.truncateTo(maxLength: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): CharSequence {
    val difference = length - maxLength
    if (difference <= 0) return this
    val trailingWhitespaces = trailingWhitespaces
    if (trailingWhitespaces.isNotEmpty()) {
        val trimmed = this.take(length - trailingWhitespaces.length.coerceAtMost(difference))
        return if (trimmed.length <= maxLength) trimmed else trimmed.truncateTo(maxLength, startIndex, minWhitespaceLength)
    }
    val regex = Regex("[${Regex.fromLiteral(Unicode.whitespaces.joinToString(""))}]{${minWhitespaceLength + 1},}")
    val longestWhitespace = regex.findAll(this, startIndex).toList().reversed().maxByOrNull { it.value.length } ?: return this

    val whitespaceStart = longestWhitespace.range.first
    val truncated = replaceRange(whitespaceStart, whitespaceStart + 2, " ")
    if (truncated.length >= length) return truncated
    return truncated.truncateTo(maxLength, startIndex, minWhitespaceLength).toString()
}
