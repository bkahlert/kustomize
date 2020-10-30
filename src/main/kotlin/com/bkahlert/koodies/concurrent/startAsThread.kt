package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun startAsThread(delay: Duration = Duration.ZERO, block: () -> Any?): Thread =
    Thread {
        delay.sleep()
        block()
    }.apply { start() }

@OptIn(ExperimentalTime::class)
fun (() -> Any?).startAsThread(delay: Duration = Duration.ZERO): Thread =
    startAsThread(delay, this)
