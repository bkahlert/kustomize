package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.nullable.invoke
import com.bkahlert.koodies.nullable.letIfSet
import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.string.prefixWith
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.string.truncateBy
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.bkahlert.koodies.terminal.ansi.Style.Companion.bold
import com.bkahlert.koodies.terminal.ansi.Style.Companion.red
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Companion.format
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status

//
open class BlockRenderingLogger<R, in HS : HasStatus>(
    private val caption: String,
    val borderedOutput: Boolean = false,
    val interceptor: (String) -> String? = { it },
    private val log: (String) -> Unit = { output: String ->
        TermUi.echo(output, trailingNewline = false)
    },
) : RenderingLogger<R, HS> {

    final override fun log(message: String, trailingNewline: Boolean) {
        val finalMessage: String? = interceptor.invoke(message + if (trailingNewline) "\n" else "")
        if (finalMessage != null) log.invoke(finalMessage)
    }

    private val blockStart
        get() = if (borderedOutput) "\n╭─────╴" + termColors.bold(caption) + "\n$prefix" else termColors.bold("Started: $caption")
    val prefix get() = if (borderedOutput) "│   " else " "
    protected val blockEnd = fun(result: Result<R>): String {
        val renderedResult: String? = result.toSingleLineString()
        val message: String =
            if (result.isSuccess) {
                val renderedSuccess = renderedResult?.let { "✔ with $it" } ?: "✔"
                if (borderedOutput) "│\n╰─────╴$renderedSuccess\n\n"
                else "Completed: $renderedSuccess\n"
            } else {
                val renderedFailure = "Failure($caption): ${renderedResult?.red()}"
                if (borderedOutput) "ϟ\n╰─────╴$renderedFailure\n\n"
                else "ϟ\n $renderedFailure\n"
            }
        return message.mapLines { it.bold() }
    }

    init {
        log(blockStart, true)
    }

    override fun logLine(output: Output, items: List<HS>): BlockRenderingLogger<R, HS> {
        if (output.unformatted.isBlank()) return this

        val currentPrefix = prefix
        output.formattedLines.forEachIndexed { index, line ->
            val message = if (index == 0 && output.type == OUT) {
                val fill = statusPadding(line.removeEscapeSequences<CharSequence>())
                val status = items.status()
                "$currentPrefix$line$fill$status"
            } else {
                "$currentPrefix$line"
            }
            log(message, true)
        }
        return this
    }

    fun logLast(result: Result<R>): R {
        return kotlin.runCatching {
            log(blockEnd(result))
            result.getOrThrow()
        }.onFailure { log(it.format(), true) }.getOrThrow()
    }

    companion object {

        private const val statusInformationColumn = 100
        private const val statusInformationMinimalPadding = 5

        val statusPadding = { text: String ->
            " ".repeat((statusInformationColumn - text.removeEscapeSequences<CharSequence>().length).coerceAtLeast(statusInformationMinimalPadding))
        }

        @Deprecated(message = "Use segment instead")
        inline fun <reified R> render(caption: String, borderedOutput: Boolean = true, block: (RenderingLogger<R, HasStatus>) -> R) {
            val logger = BlockRenderingLogger<R, HasStatus>(caption, borderedOutput)
            kotlin.runCatching { block(logger) }.also { logger.logLast(it) }.getOrThrow()
        }

        inline fun <reified R> render(caption: String): BlockRenderingLogger<R, HasStatus> = BlockRenderingLogger(caption)
    }
}


fun BlockRenderingLogger.Companion.Quiet(): BlockRenderingLogger<String?, HasStatus> =
    BlockRenderingLogger(caption = "", borderedOutput = false) { output -> /* bye bye output */ }

// TODO remove HS
inline fun <R, R2> BlockRenderingLogger<R, HasStatus>?.segment(
    caption: String,
    ansiCode: AnsiCode? = ANSI.EscapeSequences.color,
    borderedOutput: Boolean = this?.borderedOutput ?: false,
    noinline additionalInterceptor: ((String) -> String?)? = null,
    block: BlockRenderingLogger<R2, HasStatus>.() -> R2,
): R2 {
    val logger: BlockRenderingLogger<R2, HasStatus> =
        if (this == null) BlockRenderingLogger(
            caption = caption,
            borderedOutput = borderedOutput,
            interceptor = additionalInterceptor ?: { it }
        )
        else BlockRenderingLogger(
            caption = caption,
            borderedOutput = borderedOutput,
            interceptor = if (additionalInterceptor != null) {
                { it.letIfSet(additionalInterceptor).letIfSet(interceptor) }
            } else {
                interceptor
            },
        ) { output ->
            val indentedOutput = output.mapLines {
                val truncateBy = ansiCode(it.truncateBy(prefix.length, minWhitespaceLength = 3))
                truncateBy.prefixWith(prefix)
            }
            log(indentedOutput, false)
        }
    return kotlin.runCatching { block(logger) }.also { logger.logLast(it) }.getOrThrow()
}


inline fun <reified R1, reified R2> BlockRenderingLogger<R1, HasStatus>?.miniSegment(
    caption: String,
    noinline block: SingleLineLogger<R2>.() -> R2,
): R2 = if (this == null) {
    val logger: SingleLineLogger<R2> =
        object : SingleLineLogger<R2>(caption) {
            override fun log(message: String, trailingNewline: Boolean) {
                //                            TermUi.echo(message, trailingNewline = trailingNewline)
            }
        }
    kotlin.runCatching { block(logger) }.run { logger.logLast(this) }
} else {
    val logger: SingleLineLogger<R2> = object : SingleLineLogger<R2>(caption) {
        override fun log(message: String, trailingNewline: Boolean) {
            val logMessage = if (borderedOutput) "├─╴ " + termColors.bold(message) else " :" + termColors.bold(message)
            this@miniSegment.log(logMessage, true)
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logLast(it) }
}
