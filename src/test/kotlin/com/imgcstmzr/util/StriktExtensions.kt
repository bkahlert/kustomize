@file:Suppress("PublicApiImplicitType")

package com.imgcstmzr.util

import com.bkahlert.koodies.string.CodePoint
import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.string.LineSeparators.isMultiline
import com.bkahlert.koodies.string.replaceNonPrintableCharacters
import com.bkahlert.koodies.string.truncate
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.bkahlert.koodies.unit.BinaryPrefix
import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.patch.ImgOperation
import com.imgcstmzr.patch.PathOperation
import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.process.GuestfishOperation
import com.imgcstmzr.runtime.Program
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.hasSize
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun Assertion.Builder<*>.asString(trim: Boolean = true): DescribeableBuilder<String> {
    return this.get("asString") {
        val string = when (this) {
            is CodePoint -> this.string
            is Grapheme -> this.asString
            is Size -> this.toString(BinaryPrefix::class)
            is GuestfishOperation -> this.asHereDoc()
            else -> this.toString()
        }
        string.takeUnless { trim } ?: string.trim()
    }
}

fun <T : CharSequence> Assertion.Builder<T>.asBytes(trim: Boolean = true): Assertion.Builder<ByteArray> =
    asString(trim).get("as ByteArray") {
        toByteArray()
    }

fun <T : CharSequence> Assertion.Builder<T>.asByteList(trim: Boolean = true): Assertion.Builder<List<Byte>> =
    asString(trim).get("as ByteArray") {
        toByteArray().asList()
    }

fun <T : CharSequence> Assertion.Builder<T>.isEqualToByteWise(other: CharSequence) =
    assert("is equal to byte-wise") { value ->
        val thisString = value.toList()
        val otherString = other.toList()
        when (thisString.containsAll(otherString)) {
            true -> pass()
            else -> fail("\nwas        ${otherString.debug}" +
                "\ninstead of ${thisString.debug}.")
        }
    }

fun Assertion.Builder<*>.isEqualToStringWise(other: Any?, removeAnsi: Boolean = true) =
    assert("have same toString value") { value ->
        val thisString = value.toString().let { if (removeAnsi) it.removeEscapeSequences<CharSequence>() else it }
        val otherString = other.toString().let { if (removeAnsi) it.removeEscapeSequences<CharSequence>() else it }
        when (thisString == otherString) {
            true -> pass()
            else -> fail("was $otherString instead of $thisString.")
        }
    }


fun Assertion.Builder<String>.containsAtMost(value: String, limit: Int = 1) =
    assert("contains ${value.quoted} at most ${limit}x") {
        val actual = Regex.fromLiteral(value).matchEntire(it)?.groups?.size ?: 0
        if (actual <= limit) pass()
        else fail("but actually contains it ${limit}x")
    }

fun Assertion.Builder<String>.prefixes(value: String) =
    assert("prefixed by $value") { prefix ->
        if (value.startsWith(prefix)) pass()
        else fail("$value is not prefixed by ${prefix.debug}")
    }


fun <T> Assertion.Builder<List<T>>.single(assertion: Assertion.Builder<T>.() -> Unit) {
    hasSize(1).and { get { this[0] }.run(assertion) }
}

fun Assertion.Builder<File>.exists() =
    assert("exists") {
        when (it.exists()) {
            true -> pass()
            else -> fail()
        }
    }

fun Assertion.Builder<File>.isDirectory() =
    assert("is directory") {
        when (it.isDirectory) {
            true -> pass()
            else -> fail()
        }
    }


fun Assertion.Builder<File>.isWritable() =
    assert("is writable") {
        when (it.canWrite()) {
            true -> pass()
            else -> fail()
        }
    }

fun <T : CharSequence> Assertion.Builder<T>.containsOnlyCharacters(chars: CharArray) =
    assert("contains only the characters " + chars.toString().truncate(20)) {
        val unexpectedCharacters = it.filter { char: Char -> !chars.contains(char) }
        when (unexpectedCharacters.isEmpty()) {
            true -> pass()
            else -> fail("contained unexpected characters: " + unexpectedCharacters.toString().truncate(20))
        }
    }

fun Assertion.Builder<Path>.hasContent(expectedContent: String) =
    assert("has content ${expectedContent.quoted}") {
        val actualContent = it.toFile().readText()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail("was ${actualContent.quoted}")
        }
    }

fun Assertion.Builder<Path>.containsContent(expectedContent: String) =
    assert("contains content ${expectedContent.quoted}") {
        val actualContent = it.toFile().readText()
        when (actualContent.contains(expectedContent)) {
            true -> pass()
            else -> fail("was " + (if (actualContent.isMultiline) "\n$actualContent" else actualContent.quoted))
        }
    }

fun Assertion.Builder<Path>.containsContentAtMost(expectedContent: String, limit: Int = 1) =
    assert("contains content ${expectedContent.quoted} at most ${limit}x") {
        val actualContent = it.toFile().readText()
        val actual = Regex.fromLiteral(expectedContent).matchEntire(actualContent)?.groups?.size ?: 0
        if (actual <= limit) pass()
        else fail("but actually contains it ${limit}x")
    }


fun Assertion.Builder<Path>.hasEqualContent(other: Path) =
    assert("has equal content as \"$other\"") {
        val actualContent = it.readAllBytes()
        val expectedContent = other.readAllBytes()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail(
                "was ${actualContent.size} instead of ${expectedContent.size} bytes.\n" +
                    "Actual content:\n" + String(actualContent).replaceNonPrintableCharacters() + "\n" +
                    "Expected content:\n" + String(expectedContent).replaceNonPrintableCharacters() + "\n")
        }
    }

fun Assertion.Builder<Pair<Path, Path>>.haveEqualContent() =
    assert("have same content") {
        val firstContent = it.first.readAllBytes()
        val lastContent = it.second.readAllBytes()
        when (firstContent.contentEquals(lastContent)) {
            true -> pass()
            else -> fail(
                "was ${firstContent.size} instead of ${lastContent.size} bytes.\n" +
                    "Content #1:\n" + String(firstContent).replaceNonPrintableCharacters() + "\n" +
                    "Content #2:\n" + String(lastContent).replaceNonPrintableCharacters() + "\n")
        }
    }

fun Assertion.Builder<Path>.isInside(path: Path) =
    assert("is inside $path") {
        expectThat(Files.isDirectory(path))
        val relPath = path.toRealPath().relativize(it.toRealPath())
        when (relPath.none { segment -> segment.fileName.toString() == ".." }) {
            true -> pass()
            else -> fail()
        }
    }

fun Assertion.Builder<Path>.absolutePathMatches(regex: Regex) =
    assert("matches ${regex.pattern}") {
        when (it.toAbsolutePath().toString().matches(regex)) {
            true -> pass()
            else -> fail()
        }
    }

fun Assertion.Builder<Path>.isEmptyDirectory() =
    assert("is empty directory") { self ->
        val files = self.listFilesRecursively({ current -> current != self })
        when (files.isEmpty()) {
            true -> pass()
            else -> fail("contained $files")
        }
    }

@ExperimentalTime
fun Assertion.Builder<Path>.lastModified(duration: Duration) =
    assert("was last modified at most $duration ago") {
        val now = System.currentTimeMillis()
        val recent = now - duration.toLongMilliseconds()
        when (it.toFile().lastModified()) {
            in (recent..now) -> pass()
            else -> fail()
        }
    }


fun Assertion.Builder<GuestfishOperation>.withCommands(assertion: Assertion.Builder<List<String>>.() -> Unit) {
    get { commands.toList() }.run(assertion)
}

fun <T : Patch> Assertion.Builder<T>.matches(
    imgOperationsAssertion: Assertion.Builder<List<ImgOperation>>.() -> Unit = { hasSize(0) },
    guestfishOperationsAssertion: Assertion.Builder<List<GuestfishOperation>>.() -> Unit = { hasSize(0) },
    fileSystemOperationsAssertion: Assertion.Builder<List<PathOperation>>.() -> Unit = { hasSize(0) },
    programsAssertion: Assertion.Builder<List<Program>>.() -> Unit = { hasSize(0) },
) = compose("matches") { patch ->
    imgOperationsAssertion(get { imgOperations })
    guestfishOperationsAssertion(get { guestfishOperations })
    fileSystemOperationsAssertion(get { fileSystemOperations })
    programsAssertion(get { programs })
}
