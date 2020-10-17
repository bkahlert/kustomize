package com.imgcstmzr.runtime.log

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus

interface RenderingLogger<R> {

    fun logLambda(trailingNewline: Boolean, block: () -> String)

    @Deprecated("Use lambda variant", ReplaceWith("{ it }"))
    fun log(message: String, trailingNewline: Boolean) = logLambda(trailingNewline) { message }

    fun logException(block: () -> Throwable): RenderingLogger<R> = block().let {
        logLambda(true) { ERR.format(it.stackTraceToString()) }
        this
    }

    @Deprecated("Use lambda variant", ReplaceWith("{ it }"))
    fun logException(exception: Throwable): RenderingLogger<R> = logException { exception }

    fun logLineLambda(items: List<HasStatus> = emptyList(), block: () -> Output = { OUT typed "" }): RenderingLogger<R> = block().let { output ->
        logLambda(true) { output.formattedLines.joinToString("\n") }
        this
    }

    @Deprecated("Use lambda variant", ReplaceWith("{ it }"))
    fun logLine(output: Output = OUT typed "", items: List<HasStatus> = emptyList()): RenderingLogger<R> = logLineLambda(items = items) { output }

    companion object {
        val DEFAULT: RenderingLogger<Unit> = object : RenderingLogger<Unit> {
            override fun logLambda(trailingNewline: Boolean, block: () -> String) {
                block().let { TermUi.echo(it, trailingNewline) }
            }
        }
    }
}
