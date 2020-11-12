package com.bkahlert.koodies.time

import kotlin.time.Duration

fun Duration.busyWait() {
    val start = System.currentTimeMillis()
    @Suppress("ControlFlowWithEmptyBody")
    while (notPassedSince(start)) {
    }
}
