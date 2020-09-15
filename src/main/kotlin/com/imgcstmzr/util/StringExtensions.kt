package com.imgcstmzr.util

import java.net.URL
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random
import kotlin.streams.toList
import kotlin.text.contains as containsRegex
import kotlin.text.split as originalSplit

private val ansiRemovalPattern: Pattern = Pattern.compile("(?:\\x9B|\\x1B\\[)[0-?]*[ -/]*[@-~]") ?: throw IllegalStateException("Erroneous regex pattern")

/**
 * Returns the [String] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
 */
fun CharSequence.stripOffAnsi(): String = ansiRemovalPattern.matcher(this.toString()).replaceAll("")

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
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], [label] and [lineSeparator].
 */
fun hereDoc(lines: List<String>, label: String = "HERE-" + String.random(8).toUpperCase(), lineSeparator: String = System.lineSeparator()): String =
    lines.joinToString(lineSeparator, "<<$label$lineSeparator", "$lineSeparator$label")

fun CharSequence?.wrap(value: CharSequence): String = "$value$this$value"

fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left$this$right"

fun CharSequence?.quote(): String = this.wrap("\"")

fun CharSequence?.squote(): String = this.wrap("'")

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

/**
 * Masks a char sequence with a blend effect, that is every second character is replaced by [blender].
 */
fun CharSequence.blend(blender: Char): String = mapIndexed { index, char -> if (index.isEven) blender else char }.joinToString("")

/**
 * Returns true if this char sequence matches the given SLF4J / Logback style [pattern], like `I {} you have to {}`.
 *
 * Example:
 * ```
 * val pattern = "I {} you have to {}"
 * "I think you have to take a break".matches(pattern).emoji ➜ ✅
 * "I guess you do right".matches(pattern).emoji ➜ ❌
 * ```
 */
fun CharSequence.matches(pattern: String): Boolean =
    this.matches(Regex(pattern.originalSplit("{}").joinToString(".*") { Regex.escape(it) }))

val Boolean?.emoji
    inline get() = when (this) {
        true -> "✅"
        false -> "❌"
        null -> "⭕"
    }


/**
 * Centers this collection of strings by adding the needed amount of whitespaces from the left (and right)
 * of each line.
 *
 * For example:
 * ```
 * foo
 *   bar baz
 * ```
 * becomes
 * ```
 *   foo
 * bar baz
 * ```
 */
fun Collection<String>.center(whitespace: Char = '\u00A0'): List<String> {
    return map { it.trim() }.let { trimmed ->
        trimmed.maxOfOrNull { it.length }?.let { maxLength ->
            trimmed.map { line ->
                val missing: Double = (maxLength - line.stripOffAnsi().length) / 2.0
                whitespace.repeat(floor(missing).toInt()) + line + whitespace.repeat(ceil(missing).toInt())
            }.toList()
        } ?: emptyList()
    }
}

private fun Char.repeat(count: Int): String = String((1..count).map { this }.toCharArray())

/**
 * Centers this collection of strings by adding the needed amount of whitespaces from the left (and right)
 * of each line.
 *
 * For example:
 * ```
 * foo
 *   bar baz
 * ```
 * becomes
 * ```
 *   foo
 * bar baz
 * ```
 */
fun String.center(whitespace: Char = '\u00A0'): String = splitLineBreaks().center(whitespace).joinToString(System.lineSeparator())


/**
 * Centers this collection of strings by adding the needed amount of whitespaces from the left (and right)
 * of each line.
 *
 * For example:
 * ```
 * foo
 *   bar baz
 * ```
 * becomes
 * ```
 *   foo
 * bar baz
 * ```
 */
fun String.border(
    border: String = """
    ╭─╮
    │ │
    ╰─╯
""".trimIndent(),
    padding: Int = 0,
    margin: Int = 0,
): String {
    val block = splitLineBreaks().center(border[5])
    if (block.isEmpty()) return border
    val width = block[0].stripOffAnsi().length
    val height = block.size
    val bordered = "${border[0]}${border[1].repeat(width + padding * 2)}${border[2]}\n" +
        (0 until padding / 2).joinToString("") { y ->
            "${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}\n"
        } +
        (0 until height).joinToString("") { y ->
            "${border[4]}${border[5].repeat(padding)}${block[y]}${border[5].repeat(padding)}${border[6]}\n"
        } +
        (0 until padding / 2).joinToString("") { y ->
            "${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}\n"
        } +
        "${border[8]}${border[9].repeat(width + padding * 2)}${border[10]}"
    return if (margin == 0) bordered
    else bordered.border(border[5].repeat(11), padding = margin - 1, margin = 0)
}
