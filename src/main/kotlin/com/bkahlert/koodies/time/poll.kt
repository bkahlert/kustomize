@file:OptIn(ExperimentalTime::class)

package com.bkahlert.koodies.time

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Make the current [Thread] sleep for this duration.
 */
fun Duration.poll(targetState: () -> Boolean): Polling {
    require(this > Duration.ZERO) { "Interval to sleep must be positive." }
    return Polling(this, targetState)
}

class Polling(val pollInterval: Duration, val targetState: () -> Boolean) {
    var timeout: Duration = Duration.INFINITE

    fun indefinitely() = poll()

    fun forAtMost(timeout: Duration) {
        this.timeout = timeout
        poll()
    }

    fun poll() {
        val startTime = System.currentTimeMillis()
        while (!targetState() && Now.passedSince(startTime) < timeout) {
            pollInterval.sleep()
        }
    }
}
