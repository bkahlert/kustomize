package com.bkahlert.koodies.test.strikt

import com.bkahlert.koodies.string.matches
import com.bkahlert.koodies.string.withoutTrailingLineBreak
import strikt.api.Assertion
import java.io.OutputStream

@JvmName("matchesOutputStream") fun <T : OutputStream> Assertion.Builder<T>.matches(curlyPattern: String) = _matches(curlyPattern)
@JvmName("matchesCharSequence") fun <T : CharSequence> Assertion.Builder<T>.matches(curlyPattern: String) = _matches(curlyPattern)

private fun Assertion.Builder<*>._matches(curlyPattern: String): Assertion.Builder<*> {
    return assert("matches $curlyPattern") {
        val withoutTrailingLineBreak = it.toString().withoutTrailingLineBreak()
        if (withoutTrailingLineBreak.matches(curlyPattern)) pass()
        else fail()
    }
}
