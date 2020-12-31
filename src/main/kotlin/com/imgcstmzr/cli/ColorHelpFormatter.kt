package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import koodies.terminal.ANSI

class ColorHelpFormatter : CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true) {

    //    override fun renderSectionTitle(title: String) = "$prefix ${tc.bold(title)}"
    override fun renderSectionTitle(title: String) =
        (ANSI.termColors.bold + ANSI.termColors.underline)(super.renderSectionTitle(title))

    override fun renderSubcommandName(name: String) =
        (ANSI.termColors.bold + ANSI.termColors.magenta)(super.renderSubcommandName(name))

    override fun renderOptionName(name: String) =
        (ANSI.termColors.bold + ANSI.termColors.cyan)(super.renderOptionName(name))

    override fun renderArgumentName(name: String) =
        (ANSI.termColors.bold + ANSI.termColors.cyan)(super.renderArgumentName(name))

    override fun optionMetavar(option: HelpFormatter.ParameterHelp.Option) =
        ANSI.termColors.cyan(super.optionMetavar(option))

    override fun renderTag(tag: String, value: String) =
        (ANSI.termColors.bold + ANSI.termColors.gray)(super.renderTag(tag, value))
}
