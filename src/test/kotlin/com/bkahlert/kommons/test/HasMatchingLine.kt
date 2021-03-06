package com.bkahlert.kommons.test

import com.bkahlert.kommons.text.matchesCurlyPattern
import strikt.api.Assertion
import strikt.assertions.any
import java.nio.file.Path
import kotlin.io.path.readLines

fun <T : Path> Assertion.Builder<T>.hasMatchingLine(curlyPattern: String) =
    get("lines") { readLines() }.any { matchesCurlyPattern(curlyPattern) }
