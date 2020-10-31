@file:OptIn(ExperimentalTime::class)

package com.bkahlert.koodies.time

import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * Make the current [Thread] sleep for this duration.
 *
 * @sample PollSample.pollAtMostOneSecond
 */
fun Duration.poll(targetState: () -> Boolean): Polling {
    require(this > Duration.ZERO) { "Interval to sleep must be positive." }
    return Polling(this, targetState)
}

class Polling(val pollInterval: Duration, val targetState: () -> Boolean) {
    var timeout: Duration = Duration.INFINITE
    var timeoutCallback: (Duration) -> Unit = {}

    fun indefinitely() = poll()

    fun forAtMost(timeout: Duration, timeoutCallback: (Duration) -> Unit) {
        this.timeout = timeout
        this.timeoutCallback = timeoutCallback
        poll()
    }

    private fun poll() {
        val startTime = System.currentTimeMillis()
        while (!targetState() && Now.passedSince(startTime) < timeout) {
            pollInterval.sleep()
        }
        if (!targetState()) {
            timeoutCallback(Now.passedSince(startTime))
        }
    }
}

private class PollSample {
    fun pollAtMostOneSecond() {
        // poll every 100 milliseconds for a condition to become true
        // for at most 1 second
        val condition: () -> Boolean = { listOf(true, false).random() }

        100.milliseconds.poll { condition() }.forAtMost(1.seconds) { passed ->
            throw TimeoutException("Condition did not become true within $passed")
        }
    }
}
