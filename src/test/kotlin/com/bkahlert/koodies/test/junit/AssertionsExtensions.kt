package com.bkahlert.koodies.test.junit

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable
import java.util.function.Supplier
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

@ExperimentalTime
fun assertTimeoutPreemptively(
    timeout: Duration,
    executable: Executable? = null,
    messageSupplier: Supplier<String?>? = null,
) {
    Assertions.assertTimeoutPreemptively(timeout.toJavaDuration(), executable, messageSupplier)
}
