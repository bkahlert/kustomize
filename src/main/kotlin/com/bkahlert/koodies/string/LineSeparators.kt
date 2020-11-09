package com.bkahlert.koodies.string

object LineSeparators : Collection<String> {

    /**
     * Line break as used on Windows systems.
     *
     * Representations: `\r\n`,  ␍␊
     */
    const val CRLF: String = Unicode.carriageReturn + Unicode.lineFeed

    /**
     * Line break as used on Unix systems and modern Mac systems.
     *
     * Representations: `\n`, ␊, ⏎
     *
     */
    const val LF: String = Unicode.lineFeed

    /**
     * Line break as used on old Mac systems.
     *
     * Representations: `\r`,  ␍
     */
    const val CR: String = Unicode.carriageReturn

    /**
     * Line separator
     */
    const val LS: String = Unicode.lineSeparator

    /**
     * Paragraph separator
     */
    const val PS: String = Unicode.paragraphSeparator

    /**
     * Next line separator
     */
    const val NL: String = Unicode.nextLine

    @Suppress("InvisibleCharacter")
    val SEPARATOR_PATTERN by lazy { "$CRLF|[$LF$CR$LS$PS$NL]".toRegex() }

    val LAST_LINE_PATTERN by lazy { ".+$".toRegex() }

    val INTERMEDIARY_LINE_PATTERN by lazy { ".*(?<separator>${SEPARATOR_PATTERN.pattern})".toRegex() }

    val LINE_PATTERN by lazy { "${INTERMEDIARY_LINE_PATTERN.pattern}|${LAST_LINE_PATTERN.pattern}".toRegex() }

    private val ALL by lazy { arrayOf(CRLF, LF, CR, LS, PS, NL) }

    override val size: Int by lazy { ALL.size }

    override fun contains(element: String): Boolean = ALL.contains(element)

    override fun containsAll(elements: Collection<String>): Boolean = ALL.toList().containsAll(elements)

    override fun isEmpty(): Boolean = ALL.isEmpty()

    override fun iterator(): Iterator<String> = ALL.iterator()

    val MAX_LENGTH: Int by lazy { ALL.maxOf { it.length } }

    /**
     * If this [CharSequence] consists of more than one line this property is `true`.
     */
    val CharSequence.isMultiline: Boolean get() = lines().size > 1

    /**
     * Splits this char sequence to a sequence of lines delimited by any of the [LineSeparators].
     *
     * The lines returned do not include terminating line separators.
     *
     * If the last last is empty, it will be ignored unless [ignoreTrailingSeparator] is provided.
     */
    fun CharSequence.lineSequence(ignoreTrailingSeparator: Boolean = false): Sequence<String> =
        if (!ignoreTrailingSeparator) splitToSequence(*ALL)
        else splitToSequence(*ALL).iterator().run {
            generateSequence {
                hasNext().let { hasNext -> if (hasNext) next() else null }
                    ?.let { current -> if (hasNext() || current.isNotEmpty()) current else null }
            }
        }

    /**
     * Splits this char sequence to a list of lines delimited by any of the [LineSeparators].
     *
     * The lines returned do not include terminating line separators.
     *
     * If the last last is empty, it will be ignored unless [ignoreTrailingSeparator] is provided.
     */
    fun CharSequence.lines(ignoreTrailingSeparator: Boolean = false): List<String> = lineSequence(ignoreTrailingSeparator).toList()

    /**
     * If this [CharSequence] ends with one of the [LineSeparators] this property includes it.
     */
    val CharSequence.trailingLineSeparator: String? get() :String? = ALL.firstOrNull { this.endsWith(it) }

    /**
     * If this [CharSequence] ends with one of the [LineSeparators] this property is `true`.
     */
    val CharSequence.hasTrailingLineSeparator: Boolean get() = trailingLineSeparator != null

    /**
     * If this [CharSequence] ends with one of the [LineSeparators] this property contains this [CharSequence] without it.
     */
    val CharSequence.withoutTrailingLineSeparator: CharSequence
        get() = trailingLineSeparator?.let { lineBreak -> removeSuffix(lineBreak) } ?: this

    /**
     * If this [String] ends with one of the [LineSeparators] this property contains this [String] without it.
     */
    val String.withoutTrailingLineSeparator: String
        get() = trailingLineSeparator?.let { lineBreak -> removeSuffix(lineBreak) } ?: this

    // TODO would be more streamlined if there was a lines(keepDelimiters=true) function

    /**
     * If this [CharSequence] [isMultiline] this property contains the first line's line separator.
     */
    val CharSequence.firstLineSeparator: String?
        get() = lines().takeIf { it.size > 1 }.let { lines ->
            lines?.firstOrNull()?.let { firstLine ->
                LineSeparators.first { separator ->
                    val indexIncludingLineSeparator = firstLine.length + separator.length
                    if (indexIncludingLineSeparator > length) false
                    else separator == substring(firstLine.length, indexIncludingLineSeparator)
                }
            }
        }

    /**
     * If this [CharSequence] [isMultiline] this property contains the first line's line separator length.
     */
    val CharSequence.firstLineSeparatorLength: Int get() = firstLineSeparator?.length ?: 0
}
