package com.imgcstmzr.util

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.string.replaceNonPrintableCharacters
import com.bkahlert.koodies.string.wrap
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightCyan
import com.bkahlert.koodies.terminal.ansi.AnsiColors.gray

inline val CharSequence?.debug: String
    get() = if (this == null) null.wrap("❬".brightCyan(), "❭".brightCyan())
    else toString().replaceNonPrintableCharacters().wrap("❬".brightCyan(), "⫻".brightCyan() + "${this.length}".gray() + "❭".brightCyan())
inline val <T> Iterable<T>?.debug: String get() = this?.joinToString("") { it.toString().debug }.debug
inline val List<Byte>?.debug: String get() = this?.toByteArray()?.let { bytes: ByteArray -> String(bytes) }.debug
inline val Char?.debug: String get() = this.toString().replaceNonPrintableCharacters().wrap("❬", "❭")
inline val Byte?.debug: String get() = this?.let { byte: Byte -> "❬$byte=${byte.toChar().toString().replaceNonPrintableCharacters()}❭" } ?: "❬null❭"
inline val Boolean?.debug: String get() = asEmoji
inline val Any?.debug: String
    get() =
        when (this) {
            null -> "❬null❭"
            is Iterable<*> -> this.debug
            is CharSequence -> this.debug
            is ByteArray -> toList().debug
            is Byte -> this.debug
            else -> toString().debug
        }

/**
 * Creates a truncated string from selected elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [startLimit], in which case at most the first [startLimit]
 * elements and the [endLimit] last elements will be appended, leaving out the elements in between using the [truncated] string (which defaults to "...").
 */
fun <T> List<T>.joinToTruncatedString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    startLimit: Int = 2,
    endLimit: Int = 1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null,
    transformEnd: ((T) -> CharSequence)? = null,
): String {
    val limit = startLimit + endLimit
    if (size <= limit) return joinToString(separator, prefix, postfix, limit, truncated, transform)

    val list: List<T> = this.filterIndexed { index, _ -> index <= startLimit + 1 || index > size - endLimit }
    var index = 0
    return list.joinTo(StringBuilder(), separator, prefix, postfix, size, "", { element ->
        kotlin.run {
            when (index) {
                in (0 until startLimit) -> transform?.invoke(element) ?: element.toString()
                startLimit -> truncated
                else -> transformEnd?.invoke(element) ?: transform?.invoke(element) ?: element.toString()
            }
        }.also { index++ }
    }).toString()
}
