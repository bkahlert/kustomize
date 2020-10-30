package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun startAsDaemon(delay: Duration = Duration.ZERO, block: () -> Any?): Thread =
    Thread {
        delay.sleep()
        block()
    }.apply {
        isDaemon = true
        start()
    }

@OptIn(ExperimentalTime::class)
fun (() -> Any?).startAsDaemon(delay: Duration = Duration.ZERO): Thread =
    startAsDaemon(delay, this)
