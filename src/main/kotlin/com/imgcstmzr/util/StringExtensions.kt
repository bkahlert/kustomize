package com.imgcstmzr.util

import com.bkahlert.koodies.string.Unicode
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightCyan
import com.bkahlert.koodies.terminal.ansi.Style.Companion.gray
import java.net.URL
import kotlin.streams.toList
import kotlin.text.split as originalSplit

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
private val boxDrawings = Regex.escape(Unicode.boxDrawings.joinToString(""))
private val specialCharacterPattern = Regex("[^\\p{Print}\\p{IsPunctuation}$boxDrawings]")

/**
 * Replaces all special/non-printable characters, that is, all characters but \x20 (space) to \x7E (tilde) with their Unicode name.
 */
fun String.replaceNonPrintableCharacters(): String = this.replace(specialCharacterPattern) {
    val char = it.value.first()
    if (char.replacementSymbol != null) char.replacementSymbol.toString()
    else "❲" + char.unicodeName + "❳"
}


fun CharSequence?.wrap(value: CharSequence): String = "$value$this$value"
fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left$this$right"
inline val CharSequence?.quoted: String get() = this.wrap("\"")
inline val CharSequence?.singleQuoted: String get() = this.wrap("'")
inline val CharSequence?.debug: String
    get() = if (this == null) null.wrap("❬".brightCyan(), "❭".brightCyan())
    else toString().replaceNonPrintableCharacters().wrap("❬".brightCyan(), "⫻".brightCyan() + "${this.length}".gray() + "❭".brightCyan())
inline val <T> Iterable<T>?.debug: String get() = this?.joinToString("") { it.toString().debug }.debug
inline val List<Byte>?.debug: String get() = this?.toByteArray()?.let { bytes: ByteArray -> String(bytes) }.debug
inline val Char?.debug: String get() = this.toString().replaceNonPrintableCharacters().wrap("❬", "❭")
inline val Byte?.debug: String get() = this?.let { byte: Byte -> "❬$byte=${byte.toChar().toString().replaceNonPrintableCharacters()}❭" } ?: "❬null❭"
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
