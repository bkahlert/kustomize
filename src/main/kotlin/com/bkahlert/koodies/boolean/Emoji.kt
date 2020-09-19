@file:Suppress("ClassName")

package com.bkahlert.koodies.boolean

/**
 * Emoji representation of this [Boolean]
 *
 * @sample Samples.emoji.trueValue
 * @sample Samples.emoji.falseValue
 * @sample Samples.emoji.nullValue
 */
val Boolean?.emoji: String
    inline get() = when (this) {
        true -> "✅"
        false -> "❌"
        null -> "⭕"
    }

private object Samples {
    object emoji {
        fun trueValue() {
            true.emoji
        }

        fun falseValue() {
            false.emoji
        }

        fun nullValue() {
            null.emoji
        }
    }
}
