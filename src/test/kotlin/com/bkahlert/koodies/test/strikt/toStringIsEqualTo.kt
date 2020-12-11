package com.bkahlert.koodies.test.strikt

import com.bkahlert.koodies.string.CodePoint
import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.unit.BinaryPrefix
import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.guestfish.GuestfishOperation
import strikt.api.Assertion
import strikt.api.DescribeableBuilder

infix fun <T> Assertion.Builder<T>.toStringIsEqualTo(expected: String): Assertion.Builder<T> =
    assert("is equal to %s", expected) {
        when (val actual = it.toString()) {
            expected -> pass()
            else -> fail(actual = actual)
        }
    }

infix fun <T> Assertion.Builder<T>.toStringContains(expected: String): Assertion.Builder<T> =
    assert("contains %s", expected) {
        when (val actual = it.toString().contains(expected)) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

fun <T> Assertion.Builder<T>.toStringContainsAll(vararg expected: String): Assertion.Builder<T> =
    if (expected.size == 1) toStringContains(expected.single())
    else compose("contains %s", expected.joinToString(", ")) {
        expected.forEach { toStringContains(it) }
    }.then { if (allPassed && passedCount > 0) pass() else fail() }

@Deprecated("toStringIsEqualTo")
fun Assertion.Builder<*>.isEqualToStringWise(other: Any?, removeAnsi: Boolean = true) =
    assert("have same toString value") { value ->
        val actualString = value.toString().let { if (removeAnsi || value is ANSI) it.removeEscapeSequences() else it }
        val expectedString = other.toString().let { if (removeAnsi || value is ANSI) it.removeEscapeSequences() else it }
        when (actualString == expectedString) {
            true -> pass()
            else -> fail("was $actualString instead of $expectedString.")
        }
    }

@Deprecated("use toStringIsEqualTo")
fun Assertion.Builder<*>.asString(trim: Boolean = true): DescribeableBuilder<String> {
    return this.get("asString") {
        val string = when (this) {
            is CodePoint -> this.string
            is Grapheme -> this.asString
            is Size -> this.toString(BinaryPrefix::class)
            is GuestfishOperation -> this.asHereDoc().toString()
            else -> this.toString()
        }
        string.takeUnless { trim } ?: string.trim()
    }
}
