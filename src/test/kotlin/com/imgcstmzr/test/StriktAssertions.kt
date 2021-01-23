package com.imgcstmzr.test

import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exception.rootCause
import koodies.functional.alsoIf
import koodies.functional.compositionOf
import koodies.io.file.quoted
import koodies.io.file.resolveBetweenFileSystems
import koodies.io.path.asString
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.regex.namedGroups
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.gray
import koodies.terminal.AnsiColors.magenta
import koodies.text.LineSeparators.isMultiline
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.containsAny
import koodies.text.matchesCurlyPattern
import koodies.text.quoted
import koodies.unit.Size
import koodies.unit.size
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.fileName
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText


infix fun <T> Assertion.Builder<T>.toStringIsEqualTo(expected: String): Assertion.Builder<T> =
    assert("to string is equal to %s", expected) {
        when (val actual = it.toString()) {
            expected -> pass()
            else -> fail(actual = actual)
        }
    }


fun <T : Path> Assertion.Builder<T>.containsContent(expectedContent: String) =
    assert("contains content ${expectedContent.quoted}") {
        val actualContent = it.readText()
        when (actualContent.contains(expectedContent)) {
            true -> pass()
            else -> fail("was " + (if (actualContent.isMultiline) "\n$actualContent" else actualContent.quoted))
        }
    }


fun <T : Path> Assertion.Builder<T>.containsContentAtMost(expectedContent: String, limit: Int = 1) =
    assert("contains content ${expectedContent.quoted} at most ${limit}x") {
        val actualContent = it.readText()
        val actual = Regex.fromLiteral(expectedContent).matchEntire(actualContent)?.groups?.size ?: 0
        if (actual <= limit) pass()
        else fail("but actually contains it ${limit}x")
    }


fun <T : Path> Assertion.Builder<T>.hasSize(size: Size) =
    assert("has $size") {
        val actualSize = it.size
        when (actualSize == size) {
            true -> pass()
            else -> fail("was $actualSize (${actualSize.bytes} B; Δ: ${actualSize - size})")
        }
    }


fun <T : CharSequence> Assertion.Builder<T>.entirelyMatchedBy(regex: Regex) =
    get("entirely matched by $regex") { regex.matchEntire(this) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence): Assertion.Builder<MatchResult> =
    get("match entirely ${input.debug}") { matchEntire(input) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence, expected: Boolean = true): Assertion.Builder<out MatchResult?> =
    get("match entirely ${input.debug}") { matchEntire(input) }.run { if (expected) not { isNull() } else isNull() }

fun Assertion.Builder<MatchResult>.group(groupName: String) =
    get("group with name $groupName: %s") { namedGroups[groupName] }

fun Assertion.Builder<MatchResult>.group(index: Int) =
    get("group with index $index: %s") { groups[index] }

val Assertion.Builder<MatchResult>.groupValues
    get() = get("group values: %s") { groupValues }

val Assertion.Builder<MatchGroup?>.value
    get() = get("value %s") { this?.value }


/**
 * Asserts that the subject contains any of the [expected] substrings.
 */
fun <T : CharSequence> Assertion.Builder<T>.containsAny(vararg expected: T, ignoreCase: Boolean = false): Assertion.Builder<T> =
    assert("contains any of ${expected.map { it.quoted }}") {
        if (it.containsAny(expected, ignoreCase = ignoreCase)) {
            pass()
        } else {
            fail("does not contain any of ${expected.map { it.quoted }}")
        }
    }


/**
 * Maps an assertion on a [Throwable] to an assertion on its
 * [Throwable.rootCause].
 */
val <T : Throwable> Assertion.Builder<T>.rootCause: Assertion.Builder<Throwable>
    get() = get("root cause") { rootCause }


/**
 * Maps an assertion on a [Throwable] to an assertion on its
 * [Throwable.rootCause]'s [Throwable.message].
 */
val <T : Throwable> Assertion.Builder<T>.rootCauseMessage: Assertion.Builder<String?>
    get() = get("root cause message") { rootCause.message }

fun <T : Path> Assertion.Builder<T>.isDuplicateOf(expected: Path, order: Int = 1) {
    isCopyOf(expected)
    hasSameFileName(expected)
    isSiblingOf(expected)
}

fun <T : Path> Assertion.Builder<T>.isCopyOf(other: Path) =
    assert("is copy of $other") { self ->
        if (self.isRegularFile() && !other.isRegularFile()) fail("$self is a file and can only be compared to another file")
        else if (self.isDirectory() && !other.isDirectory()) fail("$self is a directory and can only be compared to another directory")
        else if (self.isDirectory()) {
            kotlin.runCatching {
                expectThat(self).hasSameFiles(other)
            }.exceptionOrNull()?.let { fail("Directories contained different files.") } ?: pass()
        } else {
            val selfBytes = self.readBytes()
            val otherBytes = other.readBytes()
            if (selfBytes.contentEquals(otherBytes)) pass()
            else fail("The resulting tarballs do not match. Expected size ${selfBytes.size} but was ${otherBytes.size}")
        }
    }


fun <T : Path> Assertion.Builder<T>.hasSameFileName(expected: Path) =
    fileName.isEqualTo(expected.fileName)


fun <T : Path> Assertion.Builder<T>.isSiblingOf(expected: Path, order: Int = 1) =
    assert("is sibling of order $order") { actual ->
        val actualNames = actual.map { name -> name.asString() }.toList()
        val otherNames = expected.map { name -> name.asString() }.toList()
        val actualIndex = actualNames.size - order - 1
        val otherIndex = otherNames.size - order - 1
        val missing = (actualIndex - otherNames.size + 1).alsoIf({ it > 0 }) {
            fail("$expected is too short. At least $it segments are missing to be able to be sibling.")
        }
        if (missing <= 0) {
            val evaluation = actualNames.zip(otherNames).mapIndexed() { index, namePair ->
                val match = if (index == actualIndex || index == otherIndex) true
                else namePair.first == namePair.second
                namePair to match
            }
            val matches = evaluation.takeWhile { (_, match) -> match }.map { (namePair, _) -> namePair.first }
            val misMatch = evaluation.getOrNull(matches.size)?.let { (namePair, _) -> namePair }
            if (misMatch != null) fail("Paths match up to $matches, then mismatch $misMatch")
            else pass()
        }
    }

fun <T : Path> Assertion.Builder<T>.hasSameFiles(other: Path) =
    assert("has same files as ${other.quoted}") { actual ->
        expectThat(actual).containsAllFiles(other)
        expectThat(other).containsAllFiles(actual)
    }

fun <T : Path> Assertion.Builder<T>.containsAllFiles(other: Path) =
    assert("contains all files as ${other.quoted}") { actual ->
        if (!actual.isDirectory()) fail("$actual is no directory")
        if (!other.isDirectory()) fail("$other is no directory")
        other.listDirectoryEntriesRecursively().filter { it.isRegularFile() }.forEach { otherPath ->
            val relativePath = other.relativize(otherPath)
            val actualPath = actual.resolveBetweenFileSystems(relativePath)
            if (actualPath.readText() != otherPath.readText()) fail("$actualPath and $otherPath have different content:\nactual: ${actual.readText()}\nexpected:${otherPath.readText()}")
        }
        pass()
    }


val <T : Path> Assertion.Builder<T>.content
    get() =
        get("content %s") { readText() }

fun <T : Path> Assertion.Builder<T>.content(init: Assertion.Builder<String>.() -> Unit) =
    content.apply(init)


fun <T : Path> Assertion.Builder<T>.hasContent(expectedContent: String) =
    assert("has content ${expectedContent.quoted}") {
        val actualContent = it.readText()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail("was ${actualContent.quoted}")
        }
    }


fun <T : CharSequence> Assertion.Builder<T>.matchesCurlyPattern(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
): Assertion.Builder<T> = assert(if (curlyPattern.isMultiline) "matches\n$curlyPattern" else "matches $curlyPattern") { actual ->
    val preprocessor = compositionOf(
        removeTrailingBreak to { s: String -> s.withoutTrailingLineSeparator },
        removeEscapeSequences to { s: String -> s.removeEscapeSequences() },
        trimmed to { s: String -> s.trim() },
    )
    var processedActual = preprocessor("$actual")
    var processedPattern = preprocessor(curlyPattern)
    if (ignoreTrailingLines) {
        val lines = processedActual.lines().size.coerceAtMost(processedPattern.lines().size)
        processedActual = processedActual.lines().take(lines).joinToString("\n")
        processedPattern = processedPattern.lines().take(lines).joinToString("\n")
    }
    if (processedActual.matchesCurlyPattern(preprocessor.invoke(curlyPattern))) pass()
    else {
        if (processedActual.lines().size == processedPattern.lines().size) {
            val analysis = processedActual.lines().zip(processedPattern.lines()).joinToString("\n\n") { (actualLine, patternLine) ->
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

private fun String.highlightTooManyLinesTo(other: String): String {
    val lines = lines()
    val tooManyStart = other.lines().size
    val sb = StringBuilder()
    lines.take(tooManyStart).forEach { sb.append(it.gray() + "\n") }
    lines.drop(tooManyStart).forEach { sb.append(it.magenta() + "\n") }
    @Suppress("ReplaceToStringWithStringTemplate")
    return sb.toString()
}
