package koodies.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable
import java.util.function.Supplier
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@Deprecated("delete")
fun <T : CharSequence> assertTimeoutPreemptively(
    timeout: Duration,
    executable: Executable,
    messageSupplier: Supplier<T>?,
) {
    Assertions.assertTimeoutPreemptively(timeout.toJavaDuration(), executable, messageSupplier?.let { "$it" })
}

@Deprecated("delete")
fun assertTimeoutPreemptively(
    timeout: Duration,
    executable: Executable,
) = assertTimeoutPreemptively<String>(timeout, executable, null)
