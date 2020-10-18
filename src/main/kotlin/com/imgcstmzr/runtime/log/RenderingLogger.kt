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

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
interface RenderingLogger<R> {

    /**
     * Method that is responsible to render what gets logged.
     *
     * All default implemented methods use this method.
     */
    fun render(trailingNewline: Boolean, block: () -> String)

    fun logException(block: () -> Throwable): RenderingLogger<R> = block().let {
        logLine { ERR.format(it.stackTraceToString()) }
        this
    }

    fun logText(block: () -> String): RenderingLogger<R> = block().let { output ->
        render(false) { output }
        this
    }

    fun logLine(block: () -> String): RenderingLogger<R> = block().let { output ->
        render(true) { output }
        this
    }

    fun logStatus(items: List<HasStatus> = emptyList(), block: () -> Output = { OUT typed "" }): RenderingLogger<R> = block().let { output ->
        logLine { output.formattedLines.joinToString("\n") }
        this
    }

    fun logResult(block: () -> Result<R>): R = kotlin.runCatching {
        val result = block()
        logLine { formatResult(result) }
        result.getOrThrow()
    }.onFailure { ex ->
        logLine { formatException(ex.toSingleLineString()) }
    }.getOrThrow()

    companion object {
        val DEFAULT: RenderingLogger<Any> = object : RenderingLogger<Any> {
            override fun render(trailingNewline: Boolean, block: () -> String) = block().let { TermUi.echo(it, trailingNewline) }
        }

        fun formatResult(result: Result<*>): String =
            if (result.isSuccess) formatReturnValue(result.toSingleLineString()) else formatException(result.toSingleLineString())

        fun formatReturnValue(oneLiner: String?): String = oneLiner?.let { "✔ ".green() + "returned".italic() + " $it" } ?: "✔".green()

        fun formatException(oneLiner: String?): String = oneLiner?.let { "ϟ ".red() + "failed with ${it.red()}" } ?: "ϟ".red()
    }
}
