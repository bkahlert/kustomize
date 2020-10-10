package com.bkahlert.koodies.string

import kotlin.math.ceil
import kotlin.math.floor

enum class TruncationStrategy(private val implementation: (CharSequence).(Int, String) -> String) {
    START({ maxLength, marker ->
        marker + drop(length - (maxLength - marker.length))
    }),
    MIDDLE({ maxLength, marker ->
        ((maxLength - marker.length) / 2.0).let { halfMaxLength ->
            val left = subSequence(0, ceil(halfMaxLength).toInt())
            val right = subSequence(length - floor(halfMaxLength).toInt(), length)
            "$left$marker$right"
        }
    }),
    END({ maxLength, marker ->
        "" + take(maxLength - marker.length) + marker
    });

    fun truncate(string: String, maxLength: Int, marker: String = "…"): String =
        if (string.length > maxLength) implementation(string, maxLength, marker) else string
}
