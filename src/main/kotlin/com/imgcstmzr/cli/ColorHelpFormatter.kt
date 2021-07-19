package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import koodies.text.ANSI.Text.Companion.ansi

class ColorHelpFormatter : CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true) {

    //    override fun renderSectionTitle(title: String) = "$prefix ${tc.bold(title)}"
    override fun renderSectionTitle(title: String): String =
        super.renderSectionTitle(title).ansi.bold.underline.done

    override fun renderSubcommandName(name: String): String =
        super.renderSubcommandName(name).ansi.magenta.bold.done

    override fun renderOptionName(name: String): String =
        super.renderOptionName(name).ansi.cyan.bold.done

    override fun renderArgumentName(name: String): String =
        super.renderArgumentName(name).ansi.cyan.bold.done

    override fun optionMetavar(option: HelpFormatter.ParameterHelp.Option): String =
        super.optionMetavar(option).ansi.cyan.done

    override fun renderTag(tag: String, value: String): String =
        super.renderTag(tag, value).ansi.gray.bold.done
}
