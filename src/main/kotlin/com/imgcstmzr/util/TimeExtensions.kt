package com.imgcstmzr.util

import com.bkahlert.koodies.number.`%+`
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

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
