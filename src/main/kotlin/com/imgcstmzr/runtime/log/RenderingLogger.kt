package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.terminal.ansi.Style.Companion.green
import com.bkahlert.koodies.terminal.ansi.Style.Companion.italic
import com.bkahlert.koodies.terminal.ansi.Style.Companion.red
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

    fun logLastLambda(block: () -> Result<R>): R = kotlin.runCatching {
        val result = block()
        logLambda(true) { formatResult(result) }
        result.getOrThrow()
    }.onFailure { ex ->
        logLambda(true) { formatException(ex.toSingleLineString()) }
    }.getOrThrow()

    @Deprecated("Use lambda variant", ReplaceWith("{ it }"))
    fun logLast(result: Result<R>): R = logLastLambda { result }

    companion object {
        val DEFAULT: RenderingLogger<Any> = object : RenderingLogger<Any> {
            override fun logLambda(trailingNewline: Boolean, block: () -> String) = block().let { TermUi.echo(it, trailingNewline) }
        }

        fun formatResult(result: Result<*>): String =
            if (result.isSuccess) formatReturnValue(result.toSingleLineString()) else formatException(result.toSingleLineString())

        fun formatReturnValue(oneLiner: String?): String = oneLiner?.let { "✔ ".green() + "returned".italic() + " $it" } ?: "✔".green()

        fun formatException(oneLiner: String?): String = oneLiner?.let { "ϟ ".red() + "failed with ${it.red()}" } ?: "ϟ".red()
    }
}
