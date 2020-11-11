package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.TruncationStrategy.MIDDLE
import com.bkahlert.koodies.string.addColumn
import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.string.prefixLinesWith
import com.bkahlert.koodies.string.prefixWith
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.string.truncate
import com.bkahlert.koodies.string.wrapLines
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiString.Companion.asAnsiString
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.formatException
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.formatResult

open class BlockRenderingLogger<R>(
    private val caption: CharSequence,
    val borderedOutput: Boolean = false,
    val statusInformationColumn: Int = 100,
    val statusInformationPadding: Int = 5,
    val statusInformationColumns: Int = 45,
    val log: (CharSequence) -> Any = { output: CharSequence ->
        TermUi.echo(output, trailingNewline = false)
    },
) : RenderingLogger<R> {

    val totalColumns = statusInformationColumn + statusInformationPadding + statusInformationColumns

    override val nestingPrefix: String get() = if (borderedOutput) "├─╴ " else " :"

    override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit = block().let { message: CharSequence ->
        val finalMessage: CharSequence? = "$message" + if (trailingNewline) "\n" else ""
        if (finalMessage != null) log.invoke(finalMessage)
    }

    private fun getStatusPadding(text: String): String =
        " ".repeat((statusInformationColumn - text.removeEscapeSequences().length).coerceAtLeast(10))

    private val blockStart
        get() = if (borderedOutput) "\n╭─────╴" + ANSI.termColors.bold("$caption") + "\n$prefix" else ANSI.termColors.bold("Started: $caption")
    val prefix: String get() = if (borderedOutput) "│   " else " "
    fun getBlockEnd(result: Result<R>): CharSequence {
        val message: String =
            if (result.isSuccess) {
                val renderedSuccess = formatResult(result)
                if (borderedOutput) "│\n╰─────╴$renderedSuccess\n"
                else "Completed: $renderedSuccess"
            } else {
                formatException(if (borderedOutput) "$LF╰─────╴" else " ", result.toSingleLineString()).toString() + if (borderedOutput) LF else ""
            }
        return message.asAnsiString().mapLines { it.bold() }
    }

    init {
        render(true) { blockStart }
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) { IO.Type.ERR.format(it.stackTraceToString()).prefixLinesWith(ignoreTrailingSeparator = false, prefix) }
    }

    override fun logLine(block: () -> CharSequence): Unit = block().let {
        render(true) {
            it.asAnsiString().wrapLines(totalColumns).prefixLinesWith(ignoreTrailingSeparator = false, prefix = prefix)
        }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> IO): Unit = block().let { output ->
        if (output.unformatted.isNotBlank()) {
            render(true) {
                val leftColumn = output.formatted.asAnsiString().wrapLines(statusInformationColumn).asAnsiString()
                val statusColumn = items.status().asAnsiString().truncate(maxLength = statusInformationColumns, MIDDLE)
                leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding).prefixLinesWith(prefix = prefix)
            }
        }
    }

    override fun logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) {
            getBlockEnd(result).asAnsiString().wrapLines(totalColumns)
        }
        return result.getOrThrow()
    }
}

@Deprecated("replaced by subLogger", ReplaceWith("subLogger"))
inline fun <R, R2> BlockRenderingLogger<R>?.segment(
    caption: CharSequence,
    ansiCode: AnsiCode? = ANSI.termColors.magenta,
    borderedOutput: Boolean = this?.borderedOutput ?: false,
    block: BlockRenderingLogger<R2>.() -> R2,
): R2 {
    val logger: BlockRenderingLogger<R2> =
        when {
            this == null -> BlockRenderingLogger(
                caption = caption,
                borderedOutput = borderedOutput,
            )
            this is MutedBlockRenderingLogger -> {
                MutedBlockRenderingLogger(
                    "$caption>",
                    borderedOutput = borderedOutput,
                )
            }
            else -> {
                BlockRenderingLogger(
                    caption = caption,
                    borderedOutput = borderedOutput,
                ) { output ->
                    val indentedOutput = output.asAnsiString().mapLines {
//                        val truncateBy = it.truncateBy(prefix.length, minWhitespaceLength = 3).let { ansiCode?.invoke(it) ?: it }
                        val truncateBy = it.replaceFirst("\\s{3}".toRegex(), "").let { ansiCode?.invoke(it) ?: it }
                        truncateBy.prefixWith(prefix)
                    }
                    logText { indentedOutput }
                }
            }
        }
    return kotlin.runCatching { block(logger) }.also { logger.logResult { it } }.getOrThrow()
}

@Deprecated("replaced by singleLineLogger", ReplaceWith("singleLineLogger"))
inline fun <reified R1, reified R2> BlockRenderingLogger<R1>?.miniSegment(
    caption: CharSequence,
    noinline block: SingleLineLogger<R2>.() -> R2,
): R2 = if (this == null) {
    val logger: SingleLineLogger<R2> =
        object : SingleLineLogger<R2>(caption) {
            override fun render(block: () -> CharSequence) {
            }
        }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
} else {
    val logger: SingleLineLogger<R2> = object : SingleLineLogger<R2>(caption) {
        override fun render(block: () -> CharSequence) {
            val message = block()
            val prefix = this@miniSegment.nestingPrefix
            val logMessage = prefix + message.bold()
            this@miniSegment.render(true) { logMessage }
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}
