package com.bkahlert.koodies.test.strikt

import com.bkahlert.koodies.boolean.emoji
import com.bkahlert.koodies.functional.compositionOf
import com.bkahlert.koodies.string.LineSeparators.isMultiline
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.matches
import com.bkahlert.koodies.terminal.ansi.Style.Companion.gray
import com.bkahlert.koodies.terminal.ansi.Style.Companion.magenta
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.imgcstmzr.util.debug
import strikt.api.Assertion
import java.io.OutputStream

@JvmName("matchesOutputStream") fun <T : OutputStream> Assertion.Builder<T>.matches(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
) = get { toString() }.internalMatches(
    curlyPattern,
    removeTrailingBreak,
    removeEscapeSequences,
    trimmed,
    ignoreTrailingLines,
)

@JvmName("matchesCharSequence") fun <T : CharSequence> Assertion.Builder<T>.matches(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
) = get { toString() }.internalMatches(
    curlyPattern,
    removeTrailingBreak,
    removeEscapeSequences,
    trimmed,
    ignoreTrailingLines,
)

@JvmName("matchesString") fun Assertion.Builder<String>.matches(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
) = get { toString() }.internalMatches(
    curlyPattern,
    removeTrailingBreak,
    removeEscapeSequences,
    trimmed,
    ignoreTrailingLines,
)

fun Assertion.Builder<String>.internalMatches(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
): Assertion.Builder<String> = assert(if (curlyPattern.isMultiline) "matches\n$curlyPattern" else "matches $curlyPattern") { actual ->
    val preprocessor = compositionOf(
        removeTrailingBreak to { s: String -> s.withoutTrailingLineSeparator },
        removeEscapeSequences to String::removeEscapeSequences,
        trimmed to String::trim,
    )
    var processedActual = preprocessor(actual)
    var processedPattern = preprocessor(curlyPattern)
    if (ignoreTrailingLines) {
        val lines = processedActual.lines().size.coerceAtMost(processedPattern.lines().size)
        processedActual = processedActual.lines().take(lines).joinToString("\n")
        processedPattern = processedPattern.lines().take(lines).joinToString("\n")
    }
    if (processedActual.matches(preprocessor.invoke(curlyPattern))) pass()
    else {
        if (processedActual.lines().size == processedPattern.lines().size) {
            val analysis = processedActual.lines().zip(processedPattern.lines()).joinToString("\n\n") { (actualLine, patternLine) ->
                val lineMatches = actualLine.matches(patternLine)
                lineMatches.emoji + "   <-\t${actualLine.debug}\nmatch?\t${patternLine.debug}"
            }
            fail(description = "\nbut was: ${if (curlyPattern.isMultiline) "\n$processedActual" else processedActual}\nAnalysis:\n$analysis")
        } else {
            if (processedActual.lines().size > processedPattern.lines().size) {
                fail(description = "\nactual has too many lines:\n${processedActual.highlightTooManyLinesTo(processedPattern)}")
            } else {
                fail(description = "\npattern has too many lines:\n${processedPattern.highlightTooManyLinesTo(processedActual)}")
            }
        }
    }
}

private fun String.highlightTooManyLinesTo(other: String): String {
    val lines = lines()
    val tooManyStart = other.lines().size
    val sb = StringBuilder()
    lines.take(tooManyStart).forEach { sb.append(it.gray() + "\n") }
    lines.drop(tooManyStart).forEach { sb.append(it.magenta() + "\n") }
    return sb.toString()
}
