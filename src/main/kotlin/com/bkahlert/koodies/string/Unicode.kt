package com.bkahlert.koodies.string

import com.bkahlert.koodies.collections.Dictionary
import com.bkahlert.koodies.collections.dictOf
import com.bkahlert.koodies.number.ApproximationMode
import com.bkahlert.koodies.number.`%+`
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.streams.toList

object Unicode : Dictionary<Long, String>
by dictOf("unicode.dict.tsv"
    .asSystemResourceUrl()
    .loadTabSeparatedValues(skipLines = 1),
    { "\\u${it.toString(16)}!!${it.toChar().category.name}" }) {

    /**
     * Returns the [CodePoint] with the specified index.
     */
    operator fun get(codePoint: Int): CodePoint = CodePoint(codePoint)

    /**
     * Returns this character's [Unicode name](https://unicode.org/charts/charindex.html).
     */
    val Char.unicodeName: String get() = Unicode[this.toLong()]

    /**
     * [ESCAPE](https://codepoints.net/U+001B)
     */
    const val escape = '\u001B'

    /**
     * [CONTROL SEQUENCE INTRODUCER](https://codepoints.net/U+009B)
     */
    const val controlSequenceIntroducer = '\u009B'

    /**
     * [CARRIAGE RETURN (CR)](https://codepoints.net/U+000D)
     */
    const val carriageReturn = "\r"

    /**
     * [LINE FEED (LF)](https://codepoints.net/U+000A)
     */
    const val lineFeed = "\n"

    /**
     * [LINE SEPARATOR](https://codepoints.net/U+2028)
     */
    const val lineSeparator = "\u2028"

    /**
     * [PARAGRAPH SEPARATOR](https://codepoints.net/U+2029)
     */
    const val paragraphSeparator = "\u2029"

    /**
     * [NEXT LINE (NEL)](https://codepoints.net/U+0085)
     */
    const val nextLine = "\u0085"

    /**
     * [RIGHT-TO-LEFT MARK](https://codepoints.net/U+200F)
     */
    const val rightToLeftMark = '\u200F'

    /**
     * [REPLACEMENT CHARACTER](https://codepoints.net/U+FFFD) �
     */
    const val replacementCharacter = '\uFFFD'

    object DivinationSymbols {
        const val tetragramForEnlargment: String = "\uD834\uDF33" // 𝌳
        const val tetragramForPurety: String = "\uD834\uDF2A" // 𝌪
    }

    var boxDrawings = ('\u2500'..'\u257F').toList()

    @Suppress("SpellCheckingInspection")
    var whitespaces: List<Char> = listOf(
        '\u0020', // SPACE: Depends on font, typically 1/4 em, often adjusted
        '\u00A0', // NO-BREAK SPACE: As a space, but often not adjusted
        '\u1680', // OGHAM SPACE MARK: Unspecified; usually not really a space but a dash
        '\u180E', // MONGOLIAN VOWEL SEPARATOR: 0
        '\u2000', // EN QUAD: 1 en (= 1/2 em)
        '\u2001', // EM QUAD: 1 em (nominally, the height of the font)
        '\u2002', // EN SPACE (nut): 1 en (= 1/2 em)
        '\u2003', // EM SPACE (mutton): 1 em
        '\u2004', // THREE-PER-EM SPACE (thick space): 1/3 em
        '\u2005', // FOUR-PER-EM SPACE (mid space): 1/4 em
        '\u2006', // SIX-PER-EM SPACE: 1/6 em
        '\u2007', // FIGURE SPACE	fo: “Tabular width”, the width of digits
        '\u2008', // PUNCTUATION SPACE: The width of a period “.”
        '\u2009', // THIN SPACE: 1/5 em (or sometimes 1/6 em)
        '\u200A', // HAIR SPACE: Narrower than THIN SPACE
        '\u200B', // ZERO WIDTH SPACE: 0
        '\u202F', // NARROW NO-BREAK SPACE	fo: Narrower than NO-BREAK SPACE (or SPACE), “typically the width of a thin space or a mid space”
        '\u205F', // MEDIUM MATHEMATICAL SPACE: 4/18 em
        '\u3000', // IDEOGRAPHIC SPACE: The width of ideographic (CJK) characters.
        '\uFEFF', // ZERO WIDTH NO-BREAK SPACE: 0
    )

    private val controlCharacters: Map<Char, Char> = mapOf(
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

    val Char.replacementSymbol: Char? get() = controlCharacters[this]

    /**
     * Unicode emojis as specified by the [Unicode® Technical Standard #51](https://unicode.org/reports/tr51/) 🤓
     */
    object Emojis {
        private val fullHourClocks = listOf("🕛", "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚").toIndexMap()
        private val halfHourClocks = listOf("🕧", "🕜", "🕝", "🕞", "🕟", "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦").toIndexMap()
        private fun List<String>.toIndexMap() = mapIndexed { index, clock -> index to clock }.toMap()

        /**
         * A dictionary that maps integers to a clock emoji that shows the corresponding full hour, e.g. `3` will return a "3 o'clock"/🕒 emoji.
         *
         * The dictionary applies the [rem] operation. Consequently all multiples of 12 of a certain hour (e.g. `15` will return a "3 o'clock"/🕒 emoji)
         * will also return the corresponding hour.
         */
        object FullHoursDictionary {
            operator fun get(key: Int): String = fullHourClocks[key `%+` fullHourClocks.size] ?: error("Missing clock in dictionary")
        }

        /**
         * A dictionary that maps integers to a clock emoji that shows the corresponding next half hour, e.g. `3` will return a "3:30 o'clock"/🕞 emoji.
         *
         * This dictionary applies the [rem] operation. Consequently all multiples of 12 of a certain hour (e.g. `15` will return a "3:30 o'clock"/🕞 emoji)
         * will also return the corresponding next half hour.
         */
        object HalfHoursDictionary {
            operator fun get(key: Int): String = halfHourClocks[key `%+` halfHourClocks.size] ?: error("Missing clock in dictionary")
        }

        fun Instant.asEmoji(approximationMode: ApproximationMode = ApproximationMode.Ceil): String {
            val zonedDateTime: ZonedDateTime = atZone(ZoneId.systemDefault())
            val hour = zonedDateTime.hour
            val minute = zonedDateTime.minute
            val closest = (approximationMode.calc(minute.toDouble(), 30.0) / 30.0).toInt()
            return listOf(FullHoursDictionary[hour - 1], HalfHoursDictionary[hour - 1], FullHoursDictionary[hour])[closest]
        }
    }

}

private fun URL.loadTabSeparatedValues(skipLines: Long) = openStream().bufferedReader().lines().skip(skipLines).map { row ->
    row.split("\t").let { java.lang.Long.parseLong(it.first(), 16) to it.last() }
}.toList().toMap()
