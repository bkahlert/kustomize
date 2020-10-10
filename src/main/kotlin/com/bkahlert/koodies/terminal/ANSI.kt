package com.bkahlert.koodies.terminal

import com.github.ajalt.mordant.TermColors

object ANSI {
    object EscapeSequences {
        const val ESC = '\u001B'

        val regex: Regex = Regex("(?:\\x9B|\\x1B\\[)[0-?]*[ -/]*[@-~]")

        val color get() = termTrueColors.hsv((Math.random() * 360.0).toInt(), 100, 94)
        val termColors by lazy { TermColors() } // TODO use extension functions
        val termNoColors by lazy { TermColors(TermColors.Level.NONE) }
        val term16Colors by lazy { TermColors(TermColors.Level.ANSI16) }
        val term256Colors by lazy { TermColors(TermColors.Level.ANSI256) }
        val termTrueColors by lazy { TermColors(TermColors.Level.TRUECOLOR) }
    }
}

val TermColors.rainbow
    get() = listOf(
        black to gray,
        red to brightRed,
        green to brightGreen,
        yellow to brightYellow,
        blue to brightBlue,
        magenta to brightMagenta,
        cyan to brightCyan,
    )
val TermColors.prefix get() = rainbow.joinToString("") { (normal, bright) -> (normal.bg + bright)("░") }
val TermColors.grayPrefix get() = rainbow.joinToString("") { (normal, _) -> (red.bg + normal)("░") }
fun TermColors.randomColor() = rainbow.map { (normal, _) -> normal }.shuffled()
fun TermColors.colorize(string: String) = string.map { randomColor().first()(it.toString()) }.joinToString("")

/**
 * Returns the [String] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
 */
fun <T : CharSequence> T.removeEscapeSequences(): String = ANSI.EscapeSequences.regex.toPattern().matcher(this.toString()).replaceAll("")
fun String.removeEscapeSequences(): String = (this as CharSequence).removeEscapeSequences()
