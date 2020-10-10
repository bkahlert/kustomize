package com.imgcstmzr.cli

import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter

class ColorHelpFormatter : CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true) {

    //    override fun renderSectionTitle(title: String) = "$prefix ${tc.bold(title)}"
    override fun renderSectionTitle(title: String) =
        (termColors.bold + termColors.underline)(super.renderSectionTitle(title))

    override fun renderSubcommandName(name: String) =
        (termColors.bold + termColors.magenta)(super.renderSubcommandName(name))

    override fun renderOptionName(name: String) =
        (termColors.bold + termColors.cyan)(super.renderOptionName(name))

    override fun renderArgumentName(name: String) =
        (termColors.bold + termColors.cyan)(super.renderArgumentName(name))

    override fun optionMetavar(option: HelpFormatter.ParameterHelp.Option) =
        termColors.cyan(super.optionMetavar(option))

    override fun renderTag(tag: String, value: String) =
        (termColors.bold + termColors.gray)(super.renderTag(tag, value))
}
