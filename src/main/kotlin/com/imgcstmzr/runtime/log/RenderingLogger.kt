package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.string.prefixWith
import com.bkahlert.koodies.string.truncateBy
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareMapLines
import com.bkahlert.koodies.terminal.ansi.AnsiColors.green
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
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
        render(true) { ERR.format(it.stackTraceToString()) }
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

    fun logStatus(items: List<HasStatus> = emptyList(), block: () -> IO = { OUT typed "" }): RenderingLogger<R> = block().let { output ->
        render(true) { output.formatted.lines().joinToString("\n") }
        this
    }

    fun logResult(block: () -> Result<R>): R = kotlin.runCatching {
        val result = block()
        render(true) { formatResult(result) }
        result.getOrThrow()
    }.onFailure { ex ->
        render(true) { formatException(ex.toSingleLineString()) }
    }.getOrThrow()

    companion object {
        val DEFAULT: RenderingLogger<Any> = object : RenderingLogger<Any> {
            override fun render(trailingNewline: Boolean, block: () -> String) = block().let { TermUi.echo(it, trailingNewline) }
        }

        fun formatResult(result: Result<*>): String =
            if (result.isSuccess) formatReturnValue(result.toSingleLineString()) else formatException(result.toSingleLineString())

        fun formatReturnValue(oneLiner: String?): String = oneLiner?.let { "✔ ".green() + "returned".italic() + " $it" } ?: "✔".green()

        fun formatException(oneLiner: String?): String = oneLiner?.let { "ϟ ".red() + "failed with ${it.red()}" } ?: "ϟ".red()

        inline fun <R, R2> RenderingLogger<R>?.subLogger(
            caption: String,
            ansiCode: AnsiCode? = null,
            borderedOutput: Boolean = (this as? BlockRenderingLogger)?.borderedOutput ?: false,
            block: RenderingLogger<R2>.() -> R2,
        ): R2 {
            val logger: RenderingLogger<R2> =
                when {
                    this == null -> BlockRenderingLogger(
                        caption = caption,
                        borderedOutput = borderedOutput,
                    )
                    this is MutedBlockRenderingLogger -> MutedBlockRenderingLogger(
                        caption = caption,
                        borderedOutput = borderedOutput,
                    )
                    else -> ((this as? BlockRenderingLogger)?.prefix ?: "$this::").let { prefix ->
                        val color = ansiCode ?: ANSI.termColors.black
                        BlockRenderingLogger(
                            caption = caption,
                            borderedOutput = borderedOutput,
                        ) { output ->
                            val indentedOutput = output.ansiAwareMapLines {
                                val truncateBy = color(it.truncateBy(prefix.length, minWhitespaceLength = 3))
                                truncateBy.prefixWith(prefix)
                            }
                            logText { indentedOutput }
                        }
                    }
                }
            return kotlin.runCatching { block(logger) }.also { logger.logResult { it } }.getOrThrow()
        }
    }
}

