@file:Suppress("ClassName")

package com.bkahlert.koodies.boolean

import com.bkahlert.koodies.string.Unicode.Emojis.checkMark_
import com.bkahlert.koodies.string.Unicode.Emojis.crossMark

/**
 * Emoji representation of this value.
 *
 * @sample Samples.emoji.trueValue
 * @sample Samples.emoji.falseValue
 * @sample Samples.emoji.nullValue
 * @sample Samples.emoji.nonNullValue
 */
val Any?.asEmoji: String
    inline get() = when (this) {
        true -> "$checkMark_"
        false -> "$crossMark"
        null -> "␀"
        else -> "🔣"
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

        fun nonNullValue() {
            "Any".asEmoji
        }
    }
}
