package com.bkahlert.koodies.time

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Duration.busyWait() {
    val start = System.currentTimeMillis()
    @Suppress("ControlFlowWithEmptyBody")
    while (notPassedSince(start)) {
    }
}
