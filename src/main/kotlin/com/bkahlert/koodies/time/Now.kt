package com.bkahlert.koodies.time

import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.string.Unicode.Emojis.Emoji
import com.bkahlert.koodies.string.Unicode.Emojis.asEmoji
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.milliseconds

object Now {
    val instant: Instant get() = Instant.now()
    val millis: Long get() = instant.toEpochMilli()
    val emoji: Emoji get() = Instant.now().asEmoji()
    val grapheme: Grapheme get() = Grapheme(Instant.now().asEmoji())

    fun passedSince(start: Long): Duration = (System.currentTimeMillis() - start).milliseconds

    operator fun plus(duration: Duration): Instant = instant.plusMillis(duration.toLongMilliseconds())

    operator fun minus(duration: Duration): Instant = instant.minusMillis(duration.toLongMilliseconds())

    override fun toString(): String = "$instant"
}
