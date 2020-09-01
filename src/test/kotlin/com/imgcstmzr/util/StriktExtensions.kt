package com.imgcstmzr.util

import strikt.api.Assertion
import strikt.api.expectThat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

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
    assert("has content \"$expectedContent\"") {
        val actualContent = it.toFile().readText()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail("was \"$actualContent\"")
        }
    }

fun Assertion.Builder<Path>.hasEqualContent(other: Path) =
    assert("has equal content as \"$other\"") {
        val actualContent = it.readAllBytes()
        val expectedContent = other.readAllBytes()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail("was \"$actualContent\"")
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
