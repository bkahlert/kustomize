package com.imgcstmzr.util

import com.bkahlert.koodies.number.`%+`
import com.bkahlert.koodies.string.Grapheme
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

object Now {
    val instant: Instant = Instant.now()
    val emoji: String = Instant.now().asEmoji()
    val grapheme: Grapheme = Grapheme(Instant.now().asEmoji())
    @OptIn(ExperimentalTime::class) fun passedSince(start: Long): Duration = (System.currentTimeMillis() - start).milliseconds
}

@OptIn(ExperimentalTime::class)
fun Duration.passedSince() = System.currentTimeMillis() - toLongMilliseconds()

@OptIn(ExperimentalTime::class)
fun Duration.passedSince(instant: Long) = passedSince() >= instant

@OptIn(ExperimentalTime::class)
fun Duration.notPassedSince(instant: Long) = passedSince() < instant

@OptIn(ExperimentalTime::class)
fun Duration.wait() {
    val start = System.currentTimeMillis()
    @Suppress("ControlFlowWithEmptyBody")
    while (notPassedSince(start)) {
    }
}

enum class ApproximationMode(val calc: (Double, Double) -> Double) {
    Ceil({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (ceil(passedNumber / roundTo) * roundTo) }),
    Floor({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (floor(passedNumber / roundTo) * roundTo) }),
    Round({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (round(passedNumber / roundTo) * roundTo) }),
}

private val fullHourClocks = listOf("🕛", "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚").toIndexMap()
private val halfHourClocks = listOf("🕧", "🕜", "🕝", "🕞", "🕟", "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦").toIndexMap()
private fun List<String>.toIndexMap() = mapIndexed { index, clock -> index to clock }.toMap()


object FullHourClockEmojisDictionary {
    operator fun get(key: Int): String = fullHourClocks[key `%+` fullHourClocks.size] ?: error("Missing clock in dictionary")
}

object HalfHourClockEmojisDictionary {
    operator fun get(key: Int): String = halfHourClocks[key `%+` halfHourClocks.size] ?: error("Missing clock in dictionary")
}

fun Instant.asEmoji(approximationMode: ApproximationMode = ApproximationMode.Ceil): String {
    val zonedDateTime: ZonedDateTime = atZone(ZoneId.systemDefault())
    val hour = zonedDateTime.hour
    val minute = zonedDateTime.minute
    val closest = (approximationMode.calc(minute.toDouble(), 30.0) / 30.0).toInt()
    return listOf(FullHourClockEmojisDictionary[hour - 1], HalfHourClockEmojisDictionary[hour - 1], FullHourClockEmojisDictionary[hour])[closest]
}
