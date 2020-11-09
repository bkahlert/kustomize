package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.LineSeparators.lineSequence
import com.bkahlert.koodies.terminal.ansi.AnsiString
import com.bkahlert.koodies.terminal.ansi.AnsiString.Companion.asAnsiString

/**
 * Returns a sequence of lines of which none is longer than [maxLineLength].
 */
fun CharSequence.linesOfLengthSequence(maxLineLength: Int, ignoreTrailingSeparator: Boolean = false): Sequence<CharSequence> {
    val ansiString = this is AnsiString
    val lines = lineSequence(ignoreTrailingSeparator = ignoreTrailingSeparator)
    return lines.flatMap { line: String ->
        if (ansiString) {
            val seq: Sequence<AnsiString> = line.asAnsiString().chunkedSequence(maxLineLength)
            if (ignoreTrailingSeparator) seq
            else seq.iterator().run { if (!hasNext()) sequenceOf(AnsiString.EMPTY) else asSequence() }
        } else {
            val seq = line.chunkedSequence(maxLineLength)
            if (ignoreTrailingSeparator) seq
            else seq.iterator().run { if (!hasNext()) sequenceOf("") else asSequence() }
        }
    }
}

/**
 * Returns a list of lines of which none is longer than [maxLineLength].
 */
fun CharSequence.linesOfLength(maxLineLength: Int, ignoreTrailingSeparator: Boolean = false): List<CharSequence> =
    linesOfLengthSequence(maxLineLength, ignoreTrailingSeparator).toList()
