package com.imgcstmzr

import strikt.api.Assertion
import strikt.api.expectThat
import java.io.File

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

fun Assertion.Builder<File>.hasContent(expectedContent: String) =
    assert("has content \"$expectedContent\"") {
        val actualContent = it.readText()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail("was \"$actualContent\"")
        }
    }

fun Assertion.Builder<File>.isInside(file: File) =
    assert("is inside $file") { it ->
        expectThat(file).isDirectory()
        val relPath = file.toPath().toRealPath().relativize(it.toPath().toRealPath())
        when (relPath.none { segment -> segment.fileName.toString() == ".." }) {
            true -> pass()
            else -> fail()
        }
    }

