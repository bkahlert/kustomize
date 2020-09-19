package com.bkahlert.koodies.test.strikt

import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.size
import strikt.api.Assertion
import java.nio.file.Path

fun Assertion.Builder<Path>.hasSize(size: Size) =
    assert("has ${size.formatted}") {
        val actualSize = it.size
        when (actualSize == size) {
            true -> pass()
            else -> fail("was ${actualSize.formatted}")
        }
    }
