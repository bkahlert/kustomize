package com.imgcstmzr.test

import koodies.regex.countMatches
import koodies.text.quoted
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable
import strikt.api.Assertion
import java.util.function.Supplier
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun <T : CharSequence> assertTimeoutPreemptively(
    timeout: Duration,
    executable: Executable,
    messageSupplier: Supplier<T>?,
) {
    Assertions.assertTimeoutPreemptively(timeout.toJavaDuration(), executable, messageSupplier?.let { "$it" })
}

fun assertTimeoutPreemptively(
    timeout: Duration,
    executable: Executable,
) = assertTimeoutPreemptively<String>(timeout, executable, null)

@Suppress("unused")
fun <T : CharSequence> Assertion.Builder<T>.containsAtLeast(value: CharSequence, lowerLimit: Int = 1) =
    assert("contains ${value.quoted} at least ${lowerLimit}x") {
        val actual = Regex.fromLiteral("$value").countMatches(it)
        if (actual >= lowerLimit) pass()
        else fail("but actually contains it ${actual}x")
    }
