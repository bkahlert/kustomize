@file:Suppress("ClassName")

package com.bkahlert.koodies.boolean

/**
 * Emoji representation of this [Boolean]
 *
 * @sample Samples.emoji.trueValue
 * @sample Samples.emoji.falseValue
 * @sample Samples.emoji.nullValue
 */
val Boolean?.asEmoji: String
    inline get() = when (this) {
        true -> "✅"
        false -> "❌"
        null -> "⭕"
    }

private object Samples {
    object emoji {
        fun trueValue() {
            true.asEmoji
        }

        fun falseValue() {
            false.asEmoji
        }

        fun nullValue() {
            null.asEmoji
        }
    }
}
