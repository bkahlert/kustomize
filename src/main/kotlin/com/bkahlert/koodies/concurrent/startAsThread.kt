package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import kotlin.time.Duration

fun startAsThread(delay: Duration = Duration.ZERO, block: () -> Any?): Thread =
    Thread {
        delay.sleep()
        block()
    }.apply { start() }

fun (() -> Any?).startAsThread(delay: Duration = Duration.ZERO): Thread =
    startAsThread(delay, this)
