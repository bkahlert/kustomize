package com.imgcstmzr.util

import com.github.ajalt.clikt.output.CliktConsole
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.output.defaultCliktConsole
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.META

fun TermUi.debug(
    message: Any?,
    trailingNewline: Boolean = true,
    err: Boolean = false,
    console: CliktConsole = defaultCliktConsole(),
    lineSeparator: String = console.lineSeparator,
) {
    val text = message?.toString()?.replace(Regex("\r?\n"), lineSeparator) ?: "null"
    val textWithCorrectEnd = if (trailingNewline) text + lineSeparator else text
    console.print(if (err) tc.dim(ERR.format(textWithCorrectEnd)) else tc.dim(META.format(textWithCorrectEnd)), err)
}
