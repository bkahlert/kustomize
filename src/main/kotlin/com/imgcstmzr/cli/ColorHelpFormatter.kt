package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import koodies.text.ANSI

class ColorHelpFormatter : CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true) {

    //    override fun renderSectionTitle(title: String) = "$prefix ${tc.bold(title)}"
    override fun renderSectionTitle(title: String): String =
        (ANSI.Style.bold + ANSI.Style.underline)(super.renderSectionTitle(title)).toString()

    override fun renderSubcommandName(name: String): String =
        (ANSI.Style.bold + ANSI.Colors.magenta)(super.renderSubcommandName(name)).toString()

    override fun renderOptionName(name: String): String =
        (ANSI.Style.bold + ANSI.Colors.cyan)(super.renderOptionName(name)).toString()

    override fun renderArgumentName(name: String): String =
        (ANSI.Style.bold + ANSI.Colors.cyan)(super.renderArgumentName(name)).toString()

    override fun optionMetavar(option: HelpFormatter.ParameterHelp.Option): String =
        ANSI.Colors.cyan(super.optionMetavar(option)).toString()

    override fun renderTag(tag: String, value: String): String =
        (ANSI.Style.bold + ANSI.Colors.gray)(super.renderTag(tag, value)).toString()
}
