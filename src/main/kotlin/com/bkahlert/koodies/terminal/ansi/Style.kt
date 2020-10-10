package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors as tc

class Style(val obj: CharSequence) {
    val colorize get() = Colorizer()

    companion object {
        val CharSequence.style: Style get() = Style(this)

        fun CharSequence.black() = tc.black(this.toString())
        fun CharSequence.red() = tc.red(this.toString())
        fun CharSequence.green() = tc.green(this.toString())
        fun CharSequence.yellow() = tc.yellow(this.toString())
        fun CharSequence.blue() = tc.blue(this.toString())
        fun CharSequence.magenta() = tc.magenta(this.toString())
        fun CharSequence.cyan() = tc.cyan(this.toString())
        fun CharSequence.white() = tc.white(this.toString())
        fun CharSequence.gray() = tc.gray(this.toString())
        fun CharSequence.brightRed() = tc.brightRed(this.toString())
        fun CharSequence.brightGreen() = tc.brightGreen(this.toString())
        fun CharSequence.brightYellow() = tc.brightYellow(this.toString())
        fun CharSequence.brightBlue() = tc.brightBlue(this.toString())
        fun CharSequence.brightMagenta() = tc.brightMagenta(this.toString())
        fun CharSequence.brightCyan() = tc.brightCyan(this.toString())
        fun CharSequence.brightWhite() = tc.brightWhite(this.toString())

        fun CharSequence.bold() = tc.bold(this.toString())
        fun CharSequence.dim() = tc.dim(this.toString())
        fun CharSequence.italic() = tc.italic(this.toString())
        fun CharSequence.underline() = tc.underline(this.toString())
        fun CharSequence.inverse() = tc.inverse(this.toString())
        fun CharSequence.hidden() = tc.hidden(this.toString())
        fun CharSequence.strikethrough() = tc.strikethrough(this.toString())
    }

    inner class Colorizer {
        fun black() = tc.black(obj.toString())
        fun red() = tc.red(obj.toString())
        fun green() = tc.green(obj.toString())
        fun yellow() = tc.yellow(obj.toString())
        fun blue() = tc.blue(obj.toString())
        fun magenta() = tc.magenta(obj.toString())
        fun cyan() = tc.cyan(obj.toString())
        fun white() = tc.white(obj.toString())
        fun gray() = tc.gray(obj.toString())

        fun brightRed() = tc.brightRed(obj.toString())
        fun brightGreen() = tc.brightGreen(obj.toString())
        fun brightYellow() = tc.brightYellow(obj.toString())
        fun brightBlue() = tc.brightBlue(obj.toString())
        fun brightMagenta() = tc.brightMagenta(obj.toString())
        fun brightCyan() = tc.brightCyan(obj.toString())
        fun brightWhite() = tc.brightWhite(obj.toString())
    }
}
