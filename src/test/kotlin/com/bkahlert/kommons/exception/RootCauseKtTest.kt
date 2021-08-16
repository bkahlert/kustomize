package com.bkahlert.kommons.exception

import strikt.api.Assertion

/**
 * Maps an assertion on a [Throwable] to an assertion on its
 * [Throwable.rootCause].
 */
val <T : Throwable> Assertion.Builder<T>.rootCause: Assertion.Builder<Throwable>
    get() = get("root cause") { rootCause }


/**
 * Maps an assertion on a [Throwable] to an assertion on its
 * [Throwable.rootCause]'s [Throwable.message].
 */
val <T : Throwable> Assertion.Builder<T>.rootCauseMessage: Assertion.Builder<String?>
    get() = get("root cause message") { rootCause.message }
