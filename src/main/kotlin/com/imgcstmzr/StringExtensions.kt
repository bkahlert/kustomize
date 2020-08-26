package com.imgcstmzr

import java.net.URL
import java.util.regex.Pattern
import kotlin.streams.toList
import kotlin.text.contains as originalContains

private val ansiRemovalPattern: Pattern = Pattern.compile("(?:\\x9B|\\x1B\\[)[0-?]*[ -/]*[@-~]") ?: throw IllegalStateException("Erroneous regex pattern")

/**
 * Returns the [CharSequence] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
 */
fun CharSequence.stripOffAnsi(): CharSequence = ansiRemovalPattern.matcher(this).replaceAll("")

/**
 * Returns the [String] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
 */
fun String.stripOffAnsi(): String = ansiRemovalPattern.matcher(this).replaceAll("")

/**
 * Returns the [String] truncated to [maxLength] characters including the [truncationMarker].
 */
fun String.truncate(maxLength: Int = 15, truncationMarker: String = "…"): String =
    if (length > maxLength) take(maxLength - truncationMarker.length) + truncationMarker else this

/**
 * Returns if this char sequence contains the specified [other] [CharSequence] as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 * @param ignoreAnsiFormatting ANSI formatting / escapes are ignored by default. Use `false` consider escape codes as well
 */
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
operator fun CharSequence.contains(
    other: CharSequence,
    ignoreCase: Boolean = false,
    ignoreAnsiFormatting: Boolean = false,
): Boolean =
    if (ignoreAnsiFormatting)
        this.stripOffAnsi().originalContains(other.stripOffAnsi(), ignoreCase)
    else
        originalContains(other, ignoreCase)


/**
 * Directory that maps Unicode codes to their names.
 */
class UnicodeDictionary(url: URL) : Map<Long, String> by
url.openStream().bufferedReader().lines().skip(1).map({ row ->
    row.split("\t").let { java.lang.Long.parseLong(it.first(), 16) to it.last() }
}).toList().toMap()

/**
 * Lazy [UnicodeDictionary] singleton
 */
private val unicodeDictionary: Lazy<UnicodeDictionary> = lazy { UnicodeDictionary(ClassLoader.getSystemResources("flatUnicode.txt").nextElement()) }

/**
 * Returns this character's [Unicode name](https://unicode.org/charts/charindex.html).
 */
val Char.unicodeName: String
    get() = unicodeDictionary.value[this.toLong()] ?: "\\u${toLong().toString(16)}!!${category.name}"


private val specialCharacterPattern = Regex("[\\P{Print}]")

/**
 * Replaces all special/non-printable characters, that is, all characters but \x20 (space) to \x7E (tilde) with their Unicode name.
 */
fun String.replaceNonPrintableCharacters(): String = this.replace(specialCharacterPattern) {
    "❲" + it.value.first().unicodeName + "❳"
}
