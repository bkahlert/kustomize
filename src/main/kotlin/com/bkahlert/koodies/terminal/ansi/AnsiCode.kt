package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.nullable.let
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.lineSequence
import com.bkahlert.koodies.string.LineSeparators.trailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.Unicode
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ANSI.termColors
import com.imgcstmzr.util.namedGroups
import com.github.ajalt.mordant.AnsiCode as MordantAnsiCode

/**
 * An [AnsiCode] that provides access to the [openCodes] and [closeCode] of an [MordantAnsiCode].
 */
class AnsiCode(
    /**
     * All codes used to open a control sequence.
     */
    val openCodes: List<Int>,
    /**
     * The code needed to close a control sequence.
     */
    val closeCode: Int,
) : com.github.ajalt.mordant.AnsiCode(listOf(openCodes to closeCode)) {
    /**
     * Creates an [AnsiCode] using a [Pair] of which the [Pair.first]
     * element forms the codes used to open a control sequence and [Pair.second]
     * the code to close a control sequence.
     */
    constructor(pair: Pair<List<Int>, Int>) : this(pair.first, pair.second)

    /**
     * Creates an [AnsiCode] based on a [MordantAnsiCode] providing access
     * to the open and close codes of the latter.
     */
    constructor(mordantAnsiCode: com.github.ajalt.mordant.AnsiCode) : this(extractCodes(mordantAnsiCode))

    /**
     * Convenience view on all codes used by this [AnsiCode].
     */
    val allCodes: List<Int> by lazy { openCodes + closeCode }

    companion object {
        const val ESC = Unicode.escape // `ESC[` also called 7Bit Control Sequence Introducer
        const val CSI = Unicode.controlSequenceIntroducer
        val ansiCloseRegex = Regex("(?<CSI>$CSI|$ESC\\[)((?:\\d{1,3};?)+)m")

        /**
         * Partial Line Forward
         *
         * Deletes the line from the cursor position to the end of the line
         * (using all existing formatting).
         *
         * Usage:
         * ```
         * termColors.yellow.bg("Text on yellow background$PARTIAL_LINE_FORWARD")
         * ```
         */
        const val PARTIAL_LINE_FORWARD = "$ESC\\[K"

        private const val splitCodeMarker = "👈 ansi code splitter 👉"

        /**
         * A map that maps the open and close codes of all supported instances of [AnsiCode]
         * to their respective [AnsiCode].
         */
        private val codeToAnsiCodeMappings: Map<Int, List<AnsiCode>> by lazy {
            hashMapOf<Int, MutableList<AnsiCode>>().apply {
                with(termColors) {
                    sequenceOf(
                        arrayOf(black, red, green, yellow, blue, magenta, cyan, white, gray),
                        arrayOf(brightRed, brightGreen, brightYellow, brightBlue, brightMagenta, brightCyan, brightWhite),
                        arrayOf(reset, bold, dim, italic, underline, inverse, hidden, strikethrough)
                    )
                }
                    .flatMap { it.asSequence() }
                    .map { AnsiCode(it) }
                    .forEach {
                        it.allCodes.forEach { code ->
                            getOrPut(code, { mutableListOf() }).add(it)
                        }
                    }
            }
        }

        /**
         * [Regex] that matches an [AnsiCode].
         */
        val regex: Regex = Regex("(?<CSI>${CSI}|${ESC}\\[)(?<parameterBytes>[0-?]*)(?<intermediateBytes>[ -/]*)(?<finalByte>[@-~])")

        /**
         * Extracts the unfortunately otherwise inaccessible open and close codes of a [MordantAnsiCode].
         */
        private fun extractCodes(ansiCode: com.github.ajalt.mordant.AnsiCode): Pair<List<Int>, Int> = splitCodeMarker
            .let<String, List<String>> { ansiCode(it).split(it) }
            .map { parseAnsiCodesAsSequence(it).toList() }
            .let<List<List<Int>>, Pair<List<Int>, Int>> { (openCodes: List<Int>, closeCode: List<Int>) -> openCodes to closeCode.single() }

        /**
         * Given a char sequence a sequence of found [AnsiCode] open and close codes is returned.
         */
        private fun parseAnsiCodesAsSequence(charSequence: CharSequence): Sequence<Int> = regex.findAll(charSequence).filter {
            val intermediateBytes = it.namedGroups["intermediateBytes"]?.value ?: ""
            val lastByte = it.namedGroups["finalByte"]?.value ?: ""
            intermediateBytes.isBlank() && lastByte == "m"
        }.flatMap { result ->
            result.namedGroups["parameterBytes"]?.value?.split(";")?.mapNotNull {
                val x = kotlin.runCatching { it.toInt() }.getOrNull()
                x
            } ?: emptyList()
        }


        /**
         * Maps each line of this char sequence using using [transform] while
         * keeping the eventually [ANSI] formatting intact.
         *
         * If this strings consists of but a single line this line is mapped.
         *
         * If this string has a trailing line that trailing line is left unchanged.
         */
        fun CharSequence.ansiAwareMapLines(ignoreTrailingSeparator: Boolean = true, transform: (String) -> String): String {
            val trailingLineBreak = trailingLineSeparator
            val prefixedLines = withoutTrailingLineSeparator.ansiAwareLineSequence().joinToString("\n") { line ->
                transform(line)
            }
            return prefixedLines + (trailingLineBreak?.let {
                trailingLineBreak + (if (!ignoreTrailingSeparator) transform("") else "")
            } ?: "")
        }

        /**
         * Splits this char sequence to a sequence of lines delimited by any of the [LineSeparators]
         * while keeping an eventually [ANSI] formatting intact.
         *
         * The lines returned do not include terminating line separators.
         */
        fun CharSequence.ansiAwareLineSequence(): Sequence<String> {
            val openingCodes = mutableListOf<Int>()
            return lineSequence().map { line ->
                val currentOpeningCodes = openingCodes.toList()
                parseAnsiCodesAsSequence(line).forEach { code ->
                    val ansiCodes: List<AnsiCode> = codeToAnsiCodeMappings[code] ?: emptyList()
                    ansiCodes.forEach { ansiCode ->
                        if (ansiCode.closeCode != code) {
                            openingCodes.addAll(ansiCode.openCodes)
                        } else {
                            openingCodes.removeAll { ansiCode.openCodes.contains(it) }
                        }
                    }
                }
                if (currentOpeningCodes.isNotEmpty()) {
                    com.github.ajalt.mordant.AnsiCode(currentOpeningCodes, 0)(line)
                } else {
                    line
                }
            }
        }

        /**
         * Splits this char sequence to a list of lines delimited by any of the [LineSeparators]
         * while keeping an eventually [ANSI] formatting intact.
         *
         * The lines returned do not include terminating line separators.
         */
        fun CharSequence.ansiAwareLines(): List<String> = ansiAwareLineSequence().toList()


        /**
         * Returns the [String] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
         */
        fun <T : CharSequence> T.removeEscapeSequences(): String = regex.replace(this, "")

        /**
         * Returns the [String] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
         */
        fun String.removeEscapeSequences(): String = (this as CharSequence).removeEscapeSequences()
    }
}
