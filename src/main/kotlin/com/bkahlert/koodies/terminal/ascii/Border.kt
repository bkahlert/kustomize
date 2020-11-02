package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.string.joinLinesToString
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ascii.Borders.Block
import com.bkahlert.koodies.terminal.ascii.Borders.Double
import com.bkahlert.koodies.terminal.ascii.Borders.Heavy
import com.bkahlert.koodies.terminal.ascii.Borders.HeavyDotted
import com.bkahlert.koodies.terminal.ascii.Borders.Light
import com.bkahlert.koodies.terminal.ascii.Borders.LightDotted
import com.bkahlert.koodies.terminal.ascii.Borders.Rounded
import com.bkahlert.koodies.terminal.ascii.Borders.SpikedOutward
import com.github.ajalt.mordant.AnsiCode


/**
 * Centers this [CharSequence] and the specified [padding] and puts a [ansiCode] styled [border] around it.
 * Furthermore a [margin] can be set to distance the bordered text.
 */
fun <T : CharSequence> T.wrapWithBorder(
    border: CharSequence = Rounded,
    padding: Int = 2,
    margin: Int = 1,
    ansiCode: AnsiCode = AnsiCode(emptyList()),
): String {
    val block = this.lines().center(border[5])
    if (block.isEmpty()) return this.toString()
    val width = block[0].removeEscapeSequences().length
    val height = block.size
    val bordered = "" +
        ansiCode("${border[0]}${border[1].repeat(width + padding * 2)}${border[2]}") + "\n" +
        (0 until padding / 2).joinToString("") {
            ansiCode("${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}") + "\n"
        } +
        (0 until height).joinToString("") { y ->
            ansiCode("${border[4]}${border[5].repeat(padding)}") + block[y] + ansiCode("${border[5].repeat(padding)}${border[6]}") + "\n"
        } +
        (0 until padding / 2).joinToString("") {
            ansiCode("${border[4]}${border[5].repeat(width + padding * 2)}${border[6]}") + "\n"
        } +
        ansiCode("${border[8]}${border[9].repeat(width + padding * 2)}${border[10]}")
    return if (margin == 0) bordered
    else bordered.wrapWithBorder(border[5].repeat(11), padding = margin - 1, margin = 0, ansiCode = ansiCode)
}

/**
 * Centers this list of [CharSequence] and the specified [padding] and puts a [ansiCode] styled [border] around it.
 * Furthermore a [margin] can be set to distance the bordered text.
 */
fun <T : CharSequence> Iterable<T>.wrapWithBorder(
    border: CharSequence = Borders.Rounded,
    padding: Int = 2,
    margin: Int = 1,
    ansiCode: AnsiCode = AnsiCode(emptyList()),
): String = joinLinesToString().wrapWithBorder(border, padding, margin, ansiCode)

class Draw(val obj: CharSequence) {
    val border get() = Border()

    inner class Border {
        /**
         * ```
         *  ┌────────┐
         *  │ SAMPLE │
         *  └────────┘
         * ```
         */
        fun light(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String = obj.wrapWithBorder(Light, padding, margin, ansiCode)

        /**
         * ```
         *  ┏━━━━━━━━┓
         *  ┃ SAMPLE ┃
         *  ┗━━━━━━━━┛
         * ```
         */
        fun heavy(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String = obj.wrapWithBorder(Heavy, padding, margin, ansiCode)

        /**
         * ```
         *  ██████████
         *  █ SAMPLE █
         *  ██████████
         * ```
         */
        fun block(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String = obj.wrapWithBorder(Block, padding, margin, ansiCode)

        /**
         * ```
         *  ╔════════╗
         *  ║ SAMPLE ║
         *  ╚════════╝
         * ```
         */
        fun double(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            obj.wrapWithBorder(Double, padding, margin, ansiCode)

        /**
         * ```
         *  ╭────────╮
         *  │ SAMPLE │
         *  ╰────────╯
         * ```
         */
        fun rounded(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            obj.wrapWithBorder(Rounded, padding, margin, ansiCode)

        /**
         * ```
         *  ┌┄┄┄┄┄┄┄┄┐
         *  ┊ SAMPLE ┊
         *  └┈┄┄┄┄┄┄┄┘
         * ```
         */
        fun lightDotted(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            obj.wrapWithBorder(LightDotted, padding, margin, ansiCode)

        /**
         * ```
         *  ┏╍╍╍╍╍╍╍╍┓
         *  ┇ SAMPLE ┇
         *  ┗╍╍╍╍╍╍╍╍┛
         * ```
         */
        fun heavyDotted(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            obj.wrapWithBorder(HeavyDotted, padding, margin, ansiCode)

        /**
         * ```
         *   △△△△△△△△
         *  ◁ SAMPLE ▷
         *   ▽▽▽▽▽▽▽▽
         * ```
         */
        fun spikedOutward(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            obj.wrapWithBorder(SpikedOutward, padding, margin, ansiCode)


        /**
         * ```
         *  ◸▽▽▽▽▽▽▽▽◹
         *  ▷ SAMPLE ◁
         *  ◺△△△△△△△△◿
         * ```
         */
        fun spikedInward(padding: Int = 0, margin: Int = 0, ansiCode: AnsiCode = AnsiCode(emptyList())): String =
            obj.wrapWithBorder(Borders.SpikedInward, padding, margin, ansiCode)
    }

    companion object {
        val CharSequence.draw: Draw get() = Draw(this)
    }
}
