package com.bkahlert.koodies.unit

import java.math.BigDecimal
import kotlin.time.seconds
import kotlin.time.Duration as KotlinDuration

private val BigDecimal.seconds: KotlinDuration
    get() = toDouble().seconds

val x = 2.Mega.seconds
