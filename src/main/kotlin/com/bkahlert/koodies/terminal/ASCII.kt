package com.bkahlert.koodies.terminal

import com.bkahlert.koodies.number.isEven

object ASCII {
    object Masking {
        /**
         * Masks a char sequence with a blend effect, that is every second character is replaced by [blender].
         */
        fun CharSequence.blend(blender: Char): String = mapIndexed { index, char -> if (index.isEven) blender else char }.joinToString("")
    }
}
