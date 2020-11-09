package com.bkahlert.koodies.string

import kotlin.math.ceil
import kotlin.math.floor

enum class TruncationStrategy(private val implementation: (CharSequence).(Int, CharSequence) -> CharSequence) {
    START({ maxLength, marker ->
        "$marker${subSequence(length - (maxLength - marker.length), length)}"
    }),
    MIDDLE({ maxLength, marker ->
        ((maxLength - marker.length) / 2.0).let { halfMaxLength ->
            val left = subSequence(0, ceil(halfMaxLength).toInt())
            val right = subSequence(length - floor(halfMaxLength).toInt(), length)
            "$left$marker$right"
        }
    }),
    END({ maxLength, marker ->
        "${subSequence(0, maxLength - marker.length)}$marker"
    });

    fun truncate(text: CharSequence, maxLength: Int, marker: String = "…"): CharSequence =
        if (text.length > maxLength) implementation(text, maxLength, marker) else text
}
