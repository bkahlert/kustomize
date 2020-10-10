package com.bkahlert.koodies.test.strikt

import com.imgcstmzr.util.readAllLines
import strikt.api.Assertion
import strikt.assertions.any
import java.nio.file.Path

fun <T : Path> Assertion.Builder<T>.hasMatchingLine(curlyPattern: String) =
    get("lines") { readAllLines() }.any { matches(curlyPattern) }
