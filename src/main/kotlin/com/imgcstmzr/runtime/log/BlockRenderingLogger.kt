package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.builder.ListBuilder.Companion.buildList
import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.TruncationStrategy.MIDDLE
import com.bkahlert.koodies.string.addColumn
import com.bkahlert.koodies.string.firstLine
import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.string.otherLines
import com.bkahlert.koodies.string.prefixLinesWith
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.string.truncate
import com.bkahlert.koodies.string.wrapLines
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiString.Companion.asAnsiString
import com.github.ajalt.clikt.output.TermUi
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

    init {
        check(statusInformationColumn > 0)
        check(statusInformationPadding > 0)
        check(statusInformationColumns > 0)
    }

    val totalColumns = statusInformationColumn + statusInformationPadding + statusInformationColumns

    final override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit = block().let { message: CharSequence ->
        val finalMessage: String = "$message" + if (trailingNewline) "\n" else ""
        log.invoke(finalMessage)
    }

    private fun getStatusPadding(text: String): String =
        " ".repeat((statusInformationColumn - text.removeEscapeSequences().length).coerceAtLeast(10))

    private val blockStart: String
        get() = buildList<String> {
            if (borderedOutput) {
                +""
                +"╭─────╴${caption.firstLine.bold()}"
                caption.otherLines.forEach {
                    +"$prefix   ${it.bold()}"
                }
                +prefix
            } else {
                +"Started: ${caption.firstLine.bold()}"
                caption.otherLines.forEach {
                    +"$prefix        ${it.bold()}"
                }
            }
        }.joinToString(LF)

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

    override fun logText(block: () -> CharSequence): Unit = block().let {
        render(false) {
            it.asAnsiString().prefixLinesWith(ignoreTrailingSeparator = true, prefix = prefix)
        }
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
                val statusColumn = items.status().asAnsiString().truncate(maxLength = statusInformationColumns - 1, MIDDLE)
                leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding).prefixLinesWith(prefix = prefix)
            }
        }
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) { IO.Type.ERR.format(it.stackTraceToString()).prefixLinesWith(ignoreTrailingSeparator = false, prefix) }
    }

    var resultLogged = false

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) {
            getBlockEnd(result).asAnsiString().wrapLines(totalColumns)
        }
        return result.getOrThrow()
    }

    override fun toString(): String =
        if (!resultLogged)
            "╷".let { vline ->
                vline +
                    LF + vline + IO.Type.META.format(" no result logged yet") +
                    LF + vline
            } else IO.Type.META.format("❕ result already logged")
}
