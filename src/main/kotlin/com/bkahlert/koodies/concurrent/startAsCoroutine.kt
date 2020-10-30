package com.bkahlert.koodies.concurrent

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun startAsCoroutine(delay: Duration = Duration.ZERO, block: () -> Any?): Job =
    GlobalScope.launch {
        delay(delay.toLongMilliseconds())
        block()
    }

@OptIn(ExperimentalTime::class)
fun (() -> Any?).startAsCoroutine(delay: Duration = Duration.ZERO): Job =
    startAsCoroutine(delay, this)
