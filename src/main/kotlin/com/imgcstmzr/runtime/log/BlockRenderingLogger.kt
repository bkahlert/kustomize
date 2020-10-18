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
import com.bkahlert.koodies.terminal.ansi.Style.Companion.italic
import com.bkahlert.koodies.terminal.ansi.Style.Companion.red
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Companion.format
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status

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
        get() = if (borderedOutput) "\n╭─────╴" + termColors.bold(caption) + "\n$prefix" else termColors.bold("Started: $caption")
    val prefix: String get() = if (borderedOutput) "│   " else " "
    fun getBlockEnd(result: Result<R>): String {
        val renderedResult: String? = result.toSingleLineString()
        val message: String =
            if (result.isSuccess) {
                val renderedSuccess = renderedResult?.let { "✔ " + "returned".italic() + " $it" } ?: "✔"
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
        logLine { blockStart }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> Output): BlockRenderingLogger<R> = block().let { output ->
        if (output.unformatted.isNotBlank()) {
            val currentPrefix = prefix
            output.formattedLines.forEachIndexed { index, line ->
                val message = if (index == 0 && output.type == OUT) {
                    val fill = statusPadding(line.removeEscapeSequences<CharSequence>())
                    val status = items.status()
                    "$currentPrefix$line$fill$status"
                } else {
                    "$currentPrefix$line"
                }
                logLine { message }
            }
        }
        this
    }

    override fun logResult(block: () -> Result<R>): R = block().let { result ->
        kotlin.runCatching {
            log(getBlockEnd(result))
            result.getOrThrow()
        }.onFailure { logLine { it.format() } }.getOrThrow()
    }

    companion object {

        private const val statusInformationColumn = 100
        private const val statusInformationMinimalPadding = 5

        val statusPadding: (String) -> String = { text: String ->
            " ".repeat((statusInformationColumn - text.removeEscapeSequences<CharSequence>().length).coerceAtLeast(statusInformationMinimalPadding))
        }

        @Deprecated(message = "Use segment instead")
        inline fun <reified R> render(caption: String, borderedOutput: Boolean = true, block: (RenderingLogger<R>) -> R) {
            val logger = BlockRenderingLogger<R>(caption, borderedOutput)
            kotlin.runCatching { block(logger) }.also { logger.logResult { it } }.getOrThrow()
        }

        inline fun <reified R> render(caption: String): BlockRenderingLogger<R> = BlockRenderingLogger(caption)
    }
}

inline fun <R, R2> BlockRenderingLogger<R>?.segment(
    caption: String,
    ansiCode: AnsiCode? = ANSI.EscapeSequences.color,
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
                    val indentedOutput = output.mapLines {
                        val truncateBy = ansiCode(it.truncateBy(prefix.length, minWhitespaceLength = 3))
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
            this@miniSegment.logLine { logMessage }
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}
