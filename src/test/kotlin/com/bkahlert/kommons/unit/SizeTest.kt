package com.bkahlert.kommons.unit

import com.bkahlert.kommons.io.path.getSize
import strikt.api.Assertion
import java.nio.file.Path

val Assertion.Builder<out Path>.size get() = get { getSize() }

fun <T : Path> Assertion.Builder<T>.hasSize(size: Size) =
    assert("has $size") {
        val actualSize = it.getSize()
        when (actualSize == size) {
            true -> pass()
            else -> fail("was $actualSize (${actualSize.bytes} B; Δ: ${actualSize - size})")
        }
    }
