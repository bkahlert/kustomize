package com.bkahlert.koodies.string

val String.Companion.LINE_BREAKS: Array<String> get() = arrayOf("\r\n", "\r", "\n")
val String.Companion.LINE_BREAKS_REGEX: Regex get() = Regex(String.LINE_BREAKS.joinToString("|", "(?:", ")", transform = Regex.Companion::escape))

fun CharSequence.trailingLineBreak(): String? = String.LINE_BREAKS.firstOrNull() { this.endsWith(it) }
fun CharSequence.hasTrailingLineBreak(): Boolean = trailingLineBreak() != null
fun CharSequence.withoutTrailingLineBreak(): CharSequence = trailingLineBreak()?.let { lineBreak -> this.removeSuffix(lineBreak) } ?: ""
