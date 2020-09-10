package com.imgcstmzr.util

import java.net.URL
import java.util.regex.Pattern
import kotlin.random.Random
import kotlin.streams.toList
import kotlin.text.contains as containsRegex
import kotlin.text.split as originalSplit

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
        this.stripOffAnsi().containsRegex(other.stripOffAnsi(), ignoreCase)
    else
        containsRegex(other, ignoreCase)


/**
 * Directory that maps Unicode codes to their names.
 */
class UnicodeDictionary(url: URL) : Map<Long, String> by
url.openStream().bufferedReader().lines().skip(1).map({ row ->
    row.originalSplit("\t").let { java.lang.Long.parseLong(it.first(), 16) to it.last() }
}).toList().toMap()

/**
 * Lazy [UnicodeDictionary] singleton
 */
private val unicodeDictionary: Lazy<UnicodeDictionary> = lazy { UnicodeDictionary(ClassLoader.getSystemResources("unicode.dict.tsv").nextElement()) }

private val unicodeControlCharacterDictionary: Map<Char, Char> = mapOf(
    '\u0000' to '\u2400', // ␀
    '\u0001' to '\u2401', // ␁
    '\u0002' to '\u2402', // ␂
    '\u0003' to '\u2403', // ␃
    '\u0004' to '\u2404', // ␄
    '\u0005' to '\u2405', // ␅
    '\u0006' to '\u2406', // ␆
    '\u0007' to '\u2407', // ␇
    '\u0008' to '\u2408', // ␈
    '\u0009' to '\u2409', // ␉
    '\u000A' to '⏎',// '\u240A', // ␊
    '\u000B' to '\u240B', // ␋
    '\u000C' to '\u240C', // ␌
    '\u000D' to '\u240D', // ␍
    '\u000E' to '\u240E', // ␎
    '\u000F' to '\u240F', // ␏
    '\u0010' to '\u2410', // ␐
    '\u0011' to '\u2411', // ␑
    '\u0012' to '\u2412', // ␒
    '\u0013' to '\u2413', // ␓
    '\u0014' to '\u2414', // ␔
    '\u0015' to '\u2415', // ␕
    '\u0016' to '\u2416', // ␖
    '\u0017' to '\u2417', // ␗
    '\u0018' to '\u2418', // ␘
    '\u0019' to '\u2419', // ␙
    '\u001A' to '\u241A', // ␚
    '\u001B' to '\u241B', // ␛
    '\u001C' to '\u241C', // ␜
    '\u001D' to '\u241D', // ␝
    '\u001E' to '\u241E', // ␞
    '\u001F' to '\u241F', // ␟
    '\u007F' to '\u2421', // ␡
)

val Char.replacementSymbol: Char? get() = unicodeControlCharacterDictionary[this]

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
    val char = it.value.first()
    if (char.replacementSymbol != null) char.replacementSymbol.toString()
    else "❲" + char.unicodeName + "❳"
}

val String.Companion.LINE_BREAKS: Array<String> get() = arrayOf("\r\n", "\r", "\n")
val String.Companion.LINE_BREAKS_REGEX: Regex get() = Regex(String.LINE_BREAKS.joinToString("|", "(?:", ")", transform = Regex.Companion::escape))

/**
 * Splits this char sequence to a list of strings around occurrences of line breaks.
 *
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 */
fun CharSequence.splitLineBreaks(limit: Int = 0): List<String> =
    this.originalSplit(*String.LINE_BREAKS, limit = limit)

val CharSequence.multiline: Boolean
    get() = containsRegex(String.Companion.LINE_BREAKS_REGEX)

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting the given [lines], [label] and [lineSeparator].
 */
fun hereDoc(lines: List<String>, label: String = "_HERE_" + String.random(), lineSeparator: String = System.lineSeparator()): String =
    lines.joinToString(lineSeparator, "<<$label$lineSeparator", "$lineSeparator$label")

fun CharSequence.wrap(value: CharSequence): String = "$value$this$value"

fun CharSequence.quote(): String = this.wrap("\"")

val CharSequence.quotes: String get() = quote()

private val randomCharacterPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
fun String.Companion.random(length: Int = 16): String = (1..length).map { randomCharacterPool[Random.nextInt(0, randomCharacterPool.size)] }.joinToString("")


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
    transform: ((T) -> CharSequence)?,
    transformEnd: ((T) -> CharSequence)?,
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
