package com.bkahlert.koodies.concurrent

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

fun startAsCoroutine(delay: Duration = Duration.ZERO, block: () -> Any?): Job =
    GlobalScope.launch {
        delay(delay.toLongMilliseconds())
        block()
    }

fun (() -> Any?).startAsCoroutine(delay: Duration = Duration.ZERO): Job =
    startAsCoroutine(delay, this)
