package com.bkahlert.koodies.time

import com.bkahlert.koodies.string.Grapheme
import com.imgcstmzr.util.asEmoji
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

object Now {
    val instant: Instant = Instant.now()
    val emoji: String = Instant.now().asEmoji()
    val grapheme: Grapheme = Grapheme(Instant.now().asEmoji())
    @OptIn(ExperimentalTime::class) fun passedSince(start: Long): Duration = (System.currentTimeMillis() - start).milliseconds
}
