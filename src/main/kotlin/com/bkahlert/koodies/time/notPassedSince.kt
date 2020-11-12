package com.bkahlert.koodies.time

import kotlin.time.Duration

fun Duration.notPassedSince(instant: Long) = passedSince() < instant
