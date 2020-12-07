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

open class BlockRenderingLogger(
    private val caption: CharSequence,
    val borderedOutput: Boolean = false,
    val statusInformationColumn: Int = 100,
    val statusInformationPadding: Int = 5,
    val statusInformationColumns: Int = 45,
    val log: (String) -> Any = { output: String ->
        TermUi.echo(output, trailingNewline = false)
    },
) : RenderingLogger {

    val totalColumns = statusInformationColumn + statusInformationPadding + statusInformationColumns

    override val nestingPrefix: String get() = if (borderedOutput) "├─╴ " else " :"

    final override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit = block().let { message: CharSequence ->
        val finalMessage: String = "$message" + if (trailingNewline) "\n" else ""
        log.invoke(finalMessage)
    }

    private fun getStatusPadding(text: String): String =
        " ".repeat((statusInformationColumn - text.removeEscapeSequences().length).coerceAtLeast(10))

    private val blockStart
        get() = if (borderedOutput) "\n╭─────╴" + ANSI.termColors.bold("$caption") + "\n$prefix" else ANSI.termColors.bold("Started: $caption")
    val prefix: String get() = if (borderedOutput) "│   " else " "
    fun <R> getBlockEnd(result: Result<R>): CharSequence {
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

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) {
            getBlockEnd(result).asAnsiString().wrapLines(totalColumns)
        }
        return result.getOrThrow()
    }
}

@Deprecated("replaced by subLogger", ReplaceWith("subLogger"))
inline fun <R> BlockRenderingLogger?.segment(
    caption: CharSequence,
    ansiCode: AnsiCode? = ANSI.termColors.magenta,
    borderedOutput: Boolean = this?.borderedOutput ?: false,
    block: BlockRenderingLogger.() -> R,
): R {
    val logger: BlockRenderingLogger =
        when {
            this == null -> BlockRenderingLogger(
                caption = caption,
                borderedOutput = borderedOutput,
            )
            this is MutedRenderingLogger -> this
            else -> {
                BlockRenderingLogger(
                    caption = caption,
                    borderedOutput = borderedOutput,
                ) { output ->
                    val indentedOutput = output.asAnsiString().mapLines { line ->
                        val truncateBy = line.replaceFirst("\\s{3}".toRegex(), "").let { ansiCode?.invoke(it) ?: it }
                        truncateBy.prefixWith(prefix)
                    }
                    logText { indentedOutput }
                }
            }
        }

    val result: Result<R> = kotlin.runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}
