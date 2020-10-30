package com.bkahlert.koodies.time

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Duration.passedSince(): Long = System.currentTimeMillis() - toLongMilliseconds()

@OptIn(ExperimentalTime::class)
fun Duration.passedSince(instant: Long) = passedSince() >= instant
