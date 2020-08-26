package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.mordant.TermColors

class ColorHelpFormatter : CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true) {

    companion object {
        val INSTANCE = ColorHelpFormatter()
    }

    val tc = TermColors()
    val rainbow = listOf(
        tc.black to tc.gray,
        tc.red to tc.brightRed,
        tc.green to tc.brightGreen,
        tc.yellow to tc.brightYellow,
        tc.blue to tc.brightBlue,
        tc.magenta to tc.brightMagenta,
        tc.cyan to tc.brightCyan,
    )
    val prefix = rainbow.map { (normal, bright) -> (normal.bg + bright)("░") }.joinToString("")
    val grayPrefix = rainbow.map { (normal, bright) -> (tc.red.bg + normal)("░") }.joinToString("")
    fun randomColor() = rainbow.map { (normal, _) -> normal }.shuffled()
    fun colorize(string: String) = string.map { randomColor().first()(it.toString()) }.joinToString("")

    val SUCCESS = tc.green("0")
    fun result(code: Int): String = if (code == 0) SUCCESS else tc.red("$code")

    //    override fun renderSectionTitle(title: String) = "$prefix ${tc.bold(title)}"
    override fun renderSectionTitle(title: String) =
        (tc.bold + tc.underline)(super.renderSectionTitle(title))

    override fun renderSubcommandName(name: String) =
        (tc.bold + tc.magenta)(super.renderSubcommandName(name))

    override fun renderOptionName(name: String) =
        (tc.bold + tc.cyan)(super.renderOptionName(name))

    override fun renderArgumentName(name: String) =
        (tc.bold + tc.cyan)(super.renderArgumentName(name))

    override fun optionMetavar(option: HelpFormatter.ParameterHelp.Option) =
        tc.cyan(super.optionMetavar(option))

    override fun renderTag(tag: String, value: String) =
        (tc.bold + tc.gray)(super.renderTag(tag, value))
}
