package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.string.Grapheme.Companion.getGraphemeCount
import com.bkahlert.koodies.string.codePointSequence
import com.bkahlert.koodies.terminal.ascii.Borders.Block
import com.bkahlert.koodies.terminal.ascii.Borders.Double
import com.bkahlert.koodies.terminal.ascii.Borders.Heavy
import com.bkahlert.koodies.terminal.ascii.Borders.HeavyDotted
import com.bkahlert.koodies.terminal.ascii.Borders.Light
import com.bkahlert.koodies.terminal.ascii.Borders.LightDotted
import com.bkahlert.koodies.terminal.ascii.Borders.Rounded
import com.bkahlert.koodies.terminal.ascii.Borders.SpikedInward
import com.bkahlert.koodies.terminal.ascii.Borders.SpikedOutward

/**
 * # Borders
 *
 * ## [Light]
 * ```
 *  ┌────────┐
 *  │ SAMPLE │
 *  └────────┘
 * ```
 *
 * ## [Heavy]
 * ```
 *  ┏━━━━━━━━┓
 *  ┃ SAMPLE ┃
 *  ┗━━━━━━━━┛
 * ```
 *
 * ## [Block]
 * ```
 *  ██████████
 *  █ SAMPLE █
 *  ██████████
 * ```
 *
 * ## [Double]
 * ```
 *  ╔════════╗
 *  ║ SAMPLE ║
 *  ╚════════╝
 * ```
 *
 * ## [Rounded]
 * ```
 *  ╭────────╮
 *  │ SAMPLE │
 *  ╰────────╯
 * ```
 *
 * ## [LightDotted]
 * ```
 *  ┌┄┄┄┄┄┄┄┄┐
 *  ┊ SAMPLE ┊
 *  └┈┄┄┄┄┄┄┄┘
 * ```
 *
 * ## [HeavyDotted]
 * ```
 *  ┏╍╍╍╍╍╍╍╍┓
 *  ┇ SAMPLE ┇
 *  ┗╍╍╍╍╍╍╍╍┛
 * ```
 *
 * ## [SpikedOutward]
 * ```
 *  △△△△△△△△
 * ◁ SAMPLE ▷
 *  ▽▽▽▽▽▽▽▽
 * ```
 *
 * ## [SpikedInward]
 * ```
 *  ◸▽▽▽▽▽▽▽▽◹
 *  ▷ SAMPLE ◁
 *  ◺△△△△△△△△◿
 * ```
 */
enum class Borders(val matrix: String) : CharSequence by matrix {

    /**
     * ```
     *  ┌────────┐
     *  │ SAMPLE │
     *  └────────┘
     * ```
     */
    Light("""
        ┌─┐
        │ │
        └─┘
    """.trimIndent()),

    /**
     * ```
     *  ┏━━━━━━━━┓
     *  ┃ SAMPLE ┃
     *  ┗━━━━━━━━┛
     * ```
     */
    Heavy("""
        ┏━┓
        ┃ ┃
        ┗━┛
    """.trimIndent()),

    /**
     * ```
     *  ██████████
     *  █ SAMPLE █
     *  ██████████
     * ```
     */
    Block("""
        ███
        █ █
        ███
    """.trimIndent()),

    /**
     * ```
     *  ╔════════╗
     *  ║ SAMPLE ║
     *  ╚════════╝
     * ```
     */
    Double("""
        ╔═╗
        ║ ║
        ╚═╝
    """.trimIndent()),

    /**
     * ```
     *  ╭────────╮
     *  │ SAMPLE │
     *  ╰────────╯
     * ```
     */
    Rounded("""
        ╭─╮
        │ │
        ╰─╯
    """.trimIndent()),

    /**
     * ```
     *  ┌┄┄┄┄┄┄┄┄┐
     *  ┊ SAMPLE ┊
     *  └┈┄┄┄┄┄┄┄┘
     * ```
     */
    LightDotted("""
        ┌┄┐
        ┊ ┊
        └┈┘
    """.trimIndent()),

    /**
     * ```
     *  ┏╍╍╍╍╍╍╍╍┓
     *  ┇ SAMPLE ┇
     *  ┗╍╍╍╍╍╍╍╍┛
     * ```
     */
    HeavyDotted("""
        ┏╍┓
        ┇ ┇
        ┗╍┛
    """.trimIndent()),

    /**
     * ```
     *  △△△△△△△△
     * ◁ SAMPLE ▷
     *  ▽▽▽▽▽▽▽▽
     * ```
     */
    SpikedOutward("""
         △ 
        ◁ ▷
         ▽ 
    """.trimIndent()),

    /**
     * ```
     *  ◸▽▽▽▽▽▽▽▽◹
     *  ▷ SAMPLE ◁
     *  ◺△△△△△△△△◿
     * ```
     */
    SpikedInward("""
        ◸▽◹
        ▷ ◁
        ◺△◿
    """.trimIndent()),
    ;

    init {
        val lines = matrix.lines()
        check(lines.size == 3) { "Matrix must have exactly 3 lines. Only ${lines.size} found." }
        lines.onEach { line ->
            check(line.getGraphemeCount() == 3) {
                "Each line of the matrix must consist of exactly 3 characters. Instead " +
                    line.codePointSequence().map { "$it" + ":" + it.string }.toList() +
                    " found in $line."
            }
        }
    }
}
