package com.bkahlert.koodies.time

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Duration.notPassedSince(instant: Long) = passedSince() < instant
