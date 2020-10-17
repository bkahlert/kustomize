package com.imgcstmzr.util

import com.bkahlert.koodies.string.withSuffix
import com.github.ajalt.clikt.output.CliktConsole
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.output.defaultCliktConsole
import com.imgcstmzr.process.Output

fun TermUi.debug(
    message: Any?,
    trailingNewline: Boolean = true,
    err: Boolean = false,
    console: CliktConsole = defaultCliktConsole(),
    lineSeparator: String = console.lineSeparator,
) {
    val text = message?.toString()?.withSuffix("\n") ?: "null"
    console.print(if (err) Output.Type.ERR.format(text) else Output.Type.META.format(text), err)
}
