package com.imgcstmzr.runtime.log

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus

interface RenderingLogger<R> {

    fun log(message: String, trailingNewline: Boolean)

    fun logException(
        exception: Throwable,
    ): RenderingLogger<R> {
        log(ERR.format(exception.stackTraceToString()), true)
        return this
    }

    fun logLine(
        output: Output = OUT typed "",
        items: List<HasStatus> = emptyList(),
    ): RenderingLogger<R> {
        log(output.formattedLines.joinToString("\n"), true)
        return this
    }

    companion object {
        val DEFAULT: RenderingLogger<Unit> = object : RenderingLogger<Unit> {
            override fun log(message: String, trailingNewline: Boolean) {
                TermUi.echo(message, trailingNewline)
            }
        }
    }
}
