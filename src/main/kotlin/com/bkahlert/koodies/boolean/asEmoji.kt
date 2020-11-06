@file:Suppress("ClassName")

package com.bkahlert.koodies.boolean

import com.bkahlert.koodies.string.Unicode.Emojis.checkMark_
import com.bkahlert.koodies.string.Unicode.Emojis.crossMark
import com.bkahlert.koodies.string.Unicode.Emojis.heavyLargeCircle

/**
 * Emoji representation of this [Boolean]
 *
 * @sample Samples.emoji.trueValue
 * @sample Samples.emoji.falseValue
 * @sample Samples.emoji.nullValue
 */
val Boolean?.asEmoji: String
    inline get() = when (this) {
        true -> "$checkMark_"
        false -> "$crossMark"
        null -> "$heavyLargeCircle"
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
