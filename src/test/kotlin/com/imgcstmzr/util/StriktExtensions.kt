@file:Suppress("PublicApiImplicitType")

package com.imgcstmzr.util

import strikt.api.Assertion
import strikt.api.expectThat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun Assertion.Builder<*>.asString(trim: Boolean = true) = get("asString") { toString().takeUnless { trim } ?: toString().trim() }

fun Assertion.Builder<*>.isEqualToStringWise(other: Any?, removeAnsi: Boolean = true) =
    assert("have same toString value") { value ->
        val thisString = value.toString().let { if (removeAnsi) it.stripOffAnsi() else it }
        val otherString = other.toString().let { if (removeAnsi) it.stripOffAnsi() else it }
        when (thisString == otherString) {
            true -> pass()
            else -> fail("was $otherString instead of $thisString.")
        }
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
            else -> fail("was " + (if (actualContent.multiline) "\n$actualContent" else actualContent.quoted))
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
    assert("is inside $path") { it ->
        expectThat(Files.isDirectory(path))
        val relPath = path.toRealPath().relativize(it.toRealPath())
        when (relPath.none { segment -> segment.fileName.toString() == ".." }) {
            true -> pass()
            else -> fail()
        }
    }

fun Assertion.Builder<Path>.absolutePathMatches(regex: Regex) =
    assert("matches ${regex.pattern}") { it ->
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
