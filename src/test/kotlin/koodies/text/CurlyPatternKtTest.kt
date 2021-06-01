package koodies.text

import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.functional.compositionOf
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.isMultiline
import koodies.text.LineSeparators.mapLines
import koodies.text.LineSeparators.unify
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import strikt.api.Assertion

fun <T : CharSequence> Assertion.Builder<T>.matchesCurlyPattern(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeAnsi: Boolean = true,
    unifyWhitespaces: Boolean = true,
    trimEnd: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
): Assertion.Builder<T> = assert(if (curlyPattern.isMultiline) "matches curly pattern\n$curlyPattern" else "matches curly pattern $curlyPattern") { actual ->
    val preprocessor = compositionOf(
        true to { s: String -> unify(s) },
        removeTrailingBreak to { s: String -> s.withoutTrailingLineSeparator },
        removeAnsi to { s: String -> s.ansiRemoved },
        unifyWhitespaces to { s: String -> Whitespaces.unify(s) },
        trimEnd to { s: String -> s.mapLines { it.trimEnd() } },
        trimmed to { s: String -> s.trim() },
    )
    var processedActual = preprocessor("$actual")
    var processedPattern = preprocessor(curlyPattern)
    if (ignoreTrailingLines) {
        val lines = processedActual.lines().size.coerceAtMost(processedPattern.lines().size)
        processedActual = processedActual.lines().take(lines).joinToString(LF)
        processedPattern = processedPattern.lines().take(lines).joinToString(LF)
    }
    if (processedActual.matchesCurlyPattern(preprocessor.invoke(curlyPattern))) pass()
    else {
        if (processedActual.lines().size == processedPattern.lines().size) {
            val analysis = processedActual.lines().zip(processedPattern.lines()).joinToString("\n$LF") { (actualLine, patternLine) ->
                val lineMatches = actualLine.matchesCurlyPattern(patternLine)
                lineMatches.asEmoji + "   <-\t${actualLine.debug}\nmatch?\t${patternLine.debug}"
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

fun <T> Assertion.Builder<T>.toStringMatchesCurlyPattern(
    expected: String,
    removeTrailingBreak: Boolean = true,
    removeAnsi: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
): Assertion.Builder<String> = get { toString() }.matchesCurlyPattern(expected,
    removeTrailingBreak,
    removeAnsi,
    trimmed,
    trimmed = ignoreTrailingLines)

private fun String.highlightTooManyLinesTo(other: String): String {
    val lines = lines()
    val tooManyStart = other.lines().size
    val sb = StringBuilder()
    lines.take(tooManyStart).forEach { sb.append(it.ansi.gray.toString() + LF) }
    lines.drop(tooManyStart).forEach { sb.append(it.ansi.magenta.toString() + LF) }
    @Suppress("ReplaceToStringWithStringTemplate")
    return sb.toString()
}
