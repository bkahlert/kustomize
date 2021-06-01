package koodies.logging

import koodies.io.ByteArrayOutputStream
import koodies.text.ANSI.ansiRemoved
import strikt.api.Assertion
import strikt.api.Assertion.Builder
import strikt.api.expectThat

private val Assertion.Builder<ByteArrayOutputStream>.printed: Assertion.Builder<String>
    get() = get("printed to simulated system out") { toString() }

/**
 * Returns an [Assertion.Builder] for all log messages recorded in this [LoggingContext].
 */
val LoggingContext.expectLogged: Builder<String>
    get() = expectThat(this).logged

/**
 * Returns an [Assertion.Builder] for all log messages recorded in the asserted [LoggingContext].
 */
val Assertion.Builder<LoggingContext>.logged: Assertion.Builder<String>
    get() = get("record log messages in %s") { logged.ansiRemoved }
