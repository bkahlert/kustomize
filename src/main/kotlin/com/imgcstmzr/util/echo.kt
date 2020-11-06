package com.imgcstmzr.util

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.string.withSuffix
import com.github.ajalt.clikt.output.CliktConsole
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.output.defaultCliktConsole

fun TermUi.debug(
    message: Any?,
    trailingNewline: Boolean = true,
    err: Boolean = false,
    console: CliktConsole = defaultCliktConsole(),
    lineSeparator: String = console.lineSeparator,
) {
    val text = message?.toString()?.withSuffix("\n") ?: "null"
    val formatted = if (err) IO.Type.ERR.format(text) else IO.Type.META.format(text)
    console.print("$formatted", err)
}
