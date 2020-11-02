package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.nullable.letIfSet
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.prefixLinesWith
import com.bkahlert.koodies.string.prefixWith
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.string.truncateBy
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareMapLines
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.formatException
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.formatResult

open class BlockRenderingLogger<R>(
    private val caption: String,
    val borderedOutput: Boolean = false,
    val interceptor: (String) -> String? = { it },
    val log: (String) -> Any = { output: String ->
        TermUi.echo(output, trailingNewline = false)
    },
) : RenderingLogger<R> {

    override fun render(trailingNewline: Boolean, block: () -> String): Unit = block().let { message: String ->
        val finalMessage: String? = interceptor.invoke(message + if (trailingNewline) "\n" else "")
        if (finalMessage != null) log.invoke(finalMessage)
    }

    private val blockStart
        get() = if (borderedOutput) "\n╭─────╴" + ANSI.termColors.bold(caption) + "\n$prefix" else ANSI.termColors.bold("Started: $caption")
    val prefix: String get() = if (borderedOutput) "│   " else " "
    fun getBlockEnd(result: Result<R>): String {
        val message: String =
            if (result.isSuccess) {
                val renderedSuccess = formatResult(result)
                if (borderedOutput) "│\n╰─────╴$renderedSuccess\n"
                else "Completed: $renderedSuccess"
            } else {
                formatException(if (borderedOutput) "$LF╰─────╴" else LF, result.toSingleLineString()) + (if (borderedOutput) "$LF$LF" else LF)
            }
        return message.ansiAwareMapLines { it.bold() }
    }

    init {
        render(true) { blockStart }
    }

    override fun logException(block: () -> Throwable): RenderingLogger<R> = block().let {
        render(true) { IO.Type.ERR.format(it.stackTraceToString()).prefixLinesWith(ignoreTrailingSeparator = false, prefix) }
        this
    }

    override fun logLine(block: () -> String): RenderingLogger<R> = block().let {
        val message = it.prefixLinesWith(ignoreTrailingSeparator = false, prefix)
        render(true) { message }
        this
    }

    override fun logStatus(items: List<HasStatus>, block: () -> IO): BlockRenderingLogger<R> = block().let { output ->
        if (output.unformatted.isNotBlank()) {
            val currentPrefix = prefix
            output.formatted.lines().forEachIndexed { index, line ->
                val message = if (index == 0 && output.type == OUT) {
                    val fill = statusPadding(line.removeEscapeSequences<CharSequence>())
                    val status = items.status()
                    "$currentPrefix$line$fill$status"
                } else {
                    "$currentPrefix$line"
                }
                render(true) { message }
            }
        }
        this
    }

    override fun logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) { getBlockEnd(result) }
        return result.getOrThrow()
    }

    companion object {

        private const val statusInformationColumn = 100
        private const val statusInformationMinimalPadding = 5

        val statusPadding: (String) -> String = { text: String ->
            " ".repeat((statusInformationColumn - text.removeEscapeSequences<CharSequence>().length).coerceAtLeast(statusInformationMinimalPadding))
        }
    }
}


inline fun <R, R2> BlockRenderingLogger<R>?.segment(
    caption: String,
    ansiCode: AnsiCode? = ANSI.termColors.magenta,
    borderedOutput: Boolean = this?.borderedOutput ?: false,
    noinline additionalInterceptor: ((String) -> String?)? = null,
    block: BlockRenderingLogger<R2>.() -> R2,
): R2 {
    val logger: BlockRenderingLogger<R2> =
        when {
            this == null -> BlockRenderingLogger(
                caption = caption,
                borderedOutput = borderedOutput,
                interceptor = additionalInterceptor ?: { it }
            )
            this is MutedBlockRenderingLogger -> {
                MutedBlockRenderingLogger(
                    "$caption>",
                    borderedOutput = borderedOutput,
                    interceptor = additionalInterceptor ?: { it })
            }
            else -> {
                BlockRenderingLogger(
                    caption = caption,
                    borderedOutput = borderedOutput,
                    interceptor = if (additionalInterceptor != null) {
                        { it.letIfSet(additionalInterceptor).letIfSet(interceptor) }
                    } else {
                        interceptor
                    },
                ) { output ->
                    val indentedOutput = output.ansiAwareMapLines {
                        val truncateBy = it.truncateBy(prefix.length, minWhitespaceLength = 3).let { ansiCode?.invoke(it) ?: it }
                        truncateBy.prefixWith(prefix)
                    }
                    logText { indentedOutput }
                }
            }
        }
    return kotlin.runCatching { block(logger) }.also { logger.logResult { it } }.getOrThrow()
}


inline fun <reified R1, reified R2> BlockRenderingLogger<R1>?.miniSegment(
    caption: String,
    noinline block: SingleLineLogger<R2>.() -> R2,
): R2 = if (this == null) {
    val logger: SingleLineLogger<R2> =
        object : SingleLineLogger<R2>(caption) {
            override fun render(block: () -> String) {
            }
        }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
} else {
    val logger: SingleLineLogger<R2> = object : SingleLineLogger<R2>(caption) {
        override fun render(block: () -> String) {
            val message = block()
            val logMessage = if (borderedOutput) "├─╴ " + message.bold() else " :" + message.bold()
            this@miniSegment.render(true) { logMessage }
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}
