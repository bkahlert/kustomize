@file:OptIn(ExperimentalTime::class)

package com.bkahlert.koodies.time

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Make the current [Thread] sleep for this duration.
 */
fun Duration.sleep() {
    require(this >= Duration.ZERO) { "Duration to sleep must be 0 or more." }
    if (this > Duration.ZERO) Thread.sleep(toLongMilliseconds())
}
