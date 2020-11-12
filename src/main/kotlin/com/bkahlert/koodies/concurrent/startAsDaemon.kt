package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import kotlin.time.Duration

fun startAsDaemon(delay: Duration = Duration.ZERO, block: () -> Any?): Thread =
    Thread {
        delay.sleep()
        block()
    }.apply {
        isDaemon = true
        start()
    }

fun (() -> Any?).startAsDaemon(delay: Duration = Duration.ZERO): Thread =
    startAsDaemon(delay, this)
