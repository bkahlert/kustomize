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
     * Representations: `\n`, ␊
     *
     */
    const val LF: String = Unicode.lineFeed

    /**
     * Line break as used on old Mac systems.
     *
     * Representations: `\r`,  ␍
     */
    const val CR: String = Unicode.carriageReturn

    private val ALL by lazy { listOf(CRLF, LF, CR) }

    override val size: Int by lazy { ALL.size }

    override fun contains(element: String): Boolean = ALL.contains(element)

    override fun containsAll(elements: Collection<String>): Boolean = ALL.containsAll(elements)

    override fun isEmpty(): Boolean = ALL.isEmpty()

    override fun iterator(): Iterator<String> = ALL.iterator()

    val MAX_LENGTH: Int by lazy { ALL.maxOf { it.length } }

    /**
     * If this [CharSequence] consists of more than one line this property is `true`.
     */
    val CharSequence.isMultiline: Boolean get() = lines().size > 1

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
    val CharSequence.withoutTrailingLineSeparator: String
        get() = trailingLineSeparator?.let { lineBreak -> this.removeSuffix(lineBreak).toString() } ?: this.toString()

    // TODO would be more streamlined if there was a lines(keepDelimiters=true) function

    /**
     * If this [CharSequence] [isMultiline] this property contains the first line including its line separator length.
     */
    val CharSequence.firstLineSeparatorLength: Int
        get() = lines().takeIf { it.size > 1 }.let { lines ->
            lines?.firstOrNull()?.let { firstLine ->
                val indexIncludingLineSeparator = firstLine.length + MAX_LENGTH
                val completeLine = substring(0, indexIncludingLineSeparator.coerceAtMost(length))
                if (completeLine.endsWith(CRLF)) 2 else 1
            } ?: 0
        }
}
