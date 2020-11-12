package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.number.isEven
import com.bkahlert.koodies.string.asString
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.terminal.ascii.Boxes.FAIL
import com.bkahlert.koodies.terminal.ascii.Boxes.PILLARS
import com.bkahlert.koodies.terminal.ascii.Boxes.SINGLE_LINE_SPHERICAL
import com.bkahlert.koodies.terminal.ascii.Boxes.SPHERICAL
import com.bkahlert.koodies.terminal.ascii.Boxes.WIDE_PILLARS


/**
 * # Boxes
 *
 * ## [FAIL]
 * ```
 * ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
 * ████▌▄▌▄▐▐▌█████
 * ████▌▄▌▄▐▐▌▀████
 * ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
 * ```
 *
 * ## [SPHERICAL]
 * ```
 *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
 * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏   SPHERE BOX    ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
 *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
 * ```
 *
 * ## [SINGLE_LINE_SPHERICAL]
 * ```
 * ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  SINGLE LINE SPHERICAL  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎
 * ```
 *
 * ## [WIDE_PILLARS]
 * ```
 * █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ WIDE PILLARS  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
 * ```
 *
 * ## [PILLARS]
 * ```
 * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
 * ```
 */
enum class Boxes(private var render: (String) -> String) {
    /**
     * ```
     * ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
     * ████▌▄▌▄▐▐▌█████
     * ████▌▄▌▄▐▐▌▀████
     * ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
     * ```
     */
    FAIL({
        """
        ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
        ████▌▄▌▄▐▐▌█████
        ████▌▄▌▄▐▐▌▀████
        ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
    """.trimIndent()
    }),

    /**
     * ```
     *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
     * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏   SPHERE BOX    ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
     *   █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
     * ```
     */
    SPHERICAL({ text ->
        val fillCount = if (text.length.isEven) 14 else 15
        val paddedText = (text + '\n' + ('X'.repeat(fillCount))).center().lines().first().padEnd(fillCount)
        val fill = ' '.repeat((paddedText.length - fillCount).coerceAtLeast(0))

        """
        $sphericalLeft$fill$sphericalRight
        $sphericalMiddleLeft$paddedText$sphericalMiddleRight
        $sphericalLeft$fill$sphericalRight
        """.trimIndent()
    }),

    /**
     * ```
     * ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  SINGLE LINE SPHERICAL  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎
     * ```
     */
    SINGLE_LINE_SPHERICAL({ text ->
        " ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  $text  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  "
    }),

    /**
     * ```
     * █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ WIDE PILLARS  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
     * ```
     */
    WIDE_PILLARS({ text ->
        "█ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ $text  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █"
    }),

    /**
     * ```
     * █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
     * ```
     */
    PILLARS({ text ->
        "█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ $text  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █"
    }),
    ;

    companion object {
        private const val sphericalLeft = """  █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏ """
        private const val sphericalRight = """ ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █"""
        private const val sphericalMiddleLeft = """█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ """
        private const val sphericalMiddleRight = """  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █"""

        /**
         * Centers this [CharSequence] and puts a styled [box] (see [Boxes]) around it.
         */
        fun <T : CharSequence> T.wrapWithBox(
            box: Boxes = SPHERICAL,
        ): String = box(this)
    }

    operator fun invoke(text: CharSequence): String = render(text.asString())

    override fun toString(): String = render(name)
}
