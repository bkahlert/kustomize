package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.string.CodePoint
import com.bkahlert.koodies.string.Grapheme.Companion.getGraphemeCount
import kotlin.streams.toList

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
     *  ◸▽▽▽▽▽▽▽▽◹
     *  ▷ SAMPLE ◁
     *  ◺△△△△△△△△◿
     * ```
     */
    Spikes("""
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
                    line.codePoints().toList().map { "$it" + ":" + CodePoint(it).string }.toList() +
                    " found in $line."
            }
        }
    }
}
