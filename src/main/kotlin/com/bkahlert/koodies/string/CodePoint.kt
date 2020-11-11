package com.bkahlert.koodies.string

import com.bkahlert.koodies.number.mod
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.unit

/**
 * Representation of a Unicode code point.
 *
 * Note: As not every Unicode code point forms a proper latter so does not every letter consist of a single code point.
 * If that is what you are looking for, use [Grapheme].
 */
inline class CodePoint(val codePoint: Int) : Comparable<CodePoint> {
    constructor(charSequence: CharSequence) : this("$charSequence"
        .also { require(it.isValidCodePoint()) { "$it does not represent a single Unicode code point" } }
        .codePointAt(0))

    constructor(chars: CharArray) : this(String(chars))

    /**
     * Contains the name of this code point
     */
    val unicodeName: String get() = Unicode[codePoint.toLong()]

    val formattedName: String get() = unicodeName.unit()

    /**
     * Contains the [Char] representing this code point **if** it can be represented by a single [Char].
     *
     * Otherwise [chars] or [string] must be used.
     */
    val char: Char? get() = if (charCount == 1) chars[0] else null

    /**
     * Contains the character pointed to and represented by a [CharArray].
     */
    val chars: CharArray get() = Character.toChars(codePoint)

    /**
     * Contains the number of [Char] values needed to represent this code point.
     */
    val charCount: Int get() = Character.charCount(codePoint)

    /**
     * Determines if this code point is a
     * [Unicode high-surrogate code unit](http://www.unicode.org/glossary/#high_surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is between a high surrogate
     * @see isLowSurrogate
     */
    val isHighSurrogate: Boolean get() = char?.let { Character.isHighSurrogate(it) } ?: false

    /**
     * Determines if this code point is a
     * [Unicode high-surrogate code unit](http://www.unicode.org/glossary/#high_surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is between a high surrogate
     * @see isHighSurrogate
     */
    val isLowSurrogate: Boolean get() = char?.let { Character.isLowSurrogate(it) } ?: false

    /**
     * Determines if this code point is a
     * [Unicode high-surrogate code unit](http://www.unicode.org/glossary/#high_surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is between a high surrogate
     * @see isHighSurrogate
     */
    val isWhitespace: Boolean get() = Character.isWhitespace(codePoint) || Unicode.whitespaces.contains(char)

    /**
     * Contains the character pointed to and represented by a [String].
     */
    val string: String get() = Character.toString(codePoint)

    override fun toString(): String = string

    operator fun rangeTo(to: CodePoint): CodePointRange = CodePointRange(this, to)

    companion object {
        fun Int.isValidCodePoint(): Boolean = Character.getType(this).toByte().let {
            it != Character.PRIVATE_USE && it != Character.SURROGATE && it != Character.UNASSIGNED
        }

        /**
         * `true` if these [Char] instances represent a *single* Unicode character.
         */
        fun CharSequence.isValidCodePoint(): Boolean = let {
            val codePointCount = it.codePoints().unordered().limit(2).count()
            codePointCount == 1L && it.codePoints().findFirst().orElseThrow().isValidCodePoint()
        }

        fun count(string: CharSequence): Long = string.codePoints().count()
        fun count(string: String): Long = string.codePoints().count()
    }

    @Suppress("KDocMissingDocumentation")
    class CodePointRange(override val start: CodePoint, override val endInclusive: CodePoint) :
        CodePointProgression(start, endInclusive, 1), ClosedRange<CodePoint> {
        @Suppress("ConvertTwoComparisonsToRangeCheck") override fun contains(value: CodePoint): Boolean = first <= value && value <= last
        override fun isEmpty(): Boolean = first > last
        override fun equals(other: Any?): Boolean = other is CodePointRange && (isEmpty() && other.isEmpty() || first == other.first && last == other.last)
        override fun hashCode(): Int = if (isEmpty()) -1 else (31 * first.codePoint + last.codePoint)
        override fun toString(): String = "$first..$last"
    }

    /**
     * A progression of values of type [CodePoint].
     */
    open class CodePointProgression internal constructor(start: CodePoint, endInclusive: CodePoint, val step: Int) : Iterable<CodePoint> {
        init {
            require(step != 0) { "Step must be non-zero." }
            require(step != Int.MIN_VALUE) { "Step must be greater than Int.MIN_VALUE to avoid overflow on negation." }
        }

        /**
         * The first element in the progression.
         */
        val first: CodePoint = start

        /**
         * The last element in the progression.
         */
        val last: CodePoint = getProgressionLastElement(start, endInclusive, step)

        override fun iterator(): CodePointIterator = CodePointProgressionIterator(first, last, step)

        /** Checks if the progression is empty. */
        open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

        override fun equals(other: Any?): Boolean =
            other is CodePointProgression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

        override fun hashCode(): Int =
            if (isEmpty()) -1 else (31 * (31 * first.codePoint + last.codePoint) + step)

        override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

        companion object {
            /**
             * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
             * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
             * [step].
             *
             * No validation on passed parameters is performed. The given parameters should satisfy the condition:
             *
             * - either `step > 0` and `start <= end`,
             * - or `step < 0` and `start >= end`.
             *
             * @param start first element of the progression
             * @param end ending bound for the progression
             * @param step increment, or difference of successive elements in the progression
             * @return the final element of the progression
             * @suppress
             */
            private fun getProgressionLastElement(start: CodePoint, end: CodePoint, step: Int): CodePoint = when {
                step > 0 -> if (start.codePoint >= end.codePoint) end else end - differenceModulo(end, start, step)
                step < 0 -> if (start <= end) end else end + differenceModulo(start, end, -step)
                else -> throw kotlin.IllegalArgumentException("Step is zero.")
            }

            // (a - b) mod c
            private fun differenceModulo(a: CodePoint, b: CodePoint, c: Int): Int = (a.mod(c) - b.mod(c)).mod(c)
        }
    }

    /**
     * An iterator over a progression of values of type [CodePoint].
     * @property step the number by which the value is incremented on each step.
     */
    internal class CodePointProgressionIterator(first: CodePoint, last: CodePoint, private val step: Int) : CodePointIterator() {
        private val finalElement = last
        private var hasNext: Boolean = if (step > 0) first <= last else first >= last
        private var next = if (hasNext) first else finalElement
        override fun hasNext(): Boolean = hasNext

        override fun nextCodePoint(): CodePoint {
            val value = next
            if (value == finalElement) {
                if (!hasNext) throw NoSuchElementException()
                hasNext = false
            } else {
                next += step
            }
            return value
        }
    }

    /**
     * An iterator over a sequence of values of type [CodePoint].
     */
    abstract class CodePointIterator : Iterator<CodePoint> {
        final override fun next() = nextCodePoint()
        abstract fun nextCodePoint(): CodePoint
    }

    override operator fun compareTo(other: CodePoint): Int = codePoint.compareTo(other.codePoint)
    operator fun plus(other: CodePoint): Int = codePoint + other.codePoint
    operator fun plus(other: Int): CodePoint = CodePoint(codePoint + other)
    operator fun minus(other: CodePoint): Int = codePoint - other.codePoint
    operator fun minus(other: Int): CodePoint = CodePoint(codePoint - other)
    @Suppress("AddOperatorModifier") fun mod(other: Int): Int = codePoint.mod(other)
}
