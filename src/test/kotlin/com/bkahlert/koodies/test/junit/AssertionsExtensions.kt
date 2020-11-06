package com.bkahlert.koodies.test.junit

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable
import java.util.function.Supplier
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

@ExperimentalTime
fun <T : CharSequence> assertTimeoutPreemptively(
    timeout: Duration,
    executable: Executable,
    messageSupplier: Supplier<T>?,
) {
    Assertions.assertTimeoutPreemptively(timeout.toJavaDuration(), executable, messageSupplier?.let { "$it" })
}

@ExperimentalTime
fun assertTimeoutPreemptively(
    timeout: Duration,
    executable: Executable,
) = assertTimeoutPreemptively<String>(timeout, executable, null)
