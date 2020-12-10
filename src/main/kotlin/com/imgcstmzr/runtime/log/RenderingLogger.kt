package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.nullable.invoke
import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.string.Unicode
import com.bkahlert.koodies.string.Unicode.Emojis.heavyBallotX
import com.bkahlert.koodies.string.Unicode.Emojis.heavyCheckMark
import com.bkahlert.koodies.string.Unicode.greekSmallLetterKoppa
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.asStatus
import koodies.io.path.bufferedWriter
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
interface RenderingLogger {

    /**
     * Method that is responsible to render what gets logged.
     *
     * All default implemented methods use this method.
     */
    fun render(trailingNewline: Boolean, block: () -> CharSequence)

    /**
     * Logs raw text.
     *
     * *Please note that in contrast to the other logging methods, **no line separator is added.**.*
     */
    fun logText(block: () -> CharSequence): Unit = block().let { output ->
        render(false) { output }
    }

    /**
     * Logs a line of text.
     */
    fun logLine(block: () -> CharSequence): Unit = block().let { output ->
        render(true) { output }
    }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    fun logStatus(items: List<HasStatus> = emptyList(), block: () -> IO = { OUT typed "" }): Unit = block().let { output ->
        render(true) { "${output.formatted} (${items.size})" }
    }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    fun logStatus(vararg items: HasStatus, block: () -> IO = { OUT typed "" }): Unit =
        logStatus(items.toList(), block)

    /**
     * Logs some programs [IO] and the processed items [statuses].
     */
    fun logStatus(vararg statuses: String, block: () -> IO = { OUT typed "" }): Unit =
        logStatus(statuses.map { it.asStatus() }, block)

    /**
     * Logs the result of the process this logger is used for.
     */
    fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) { formatResult(result) }
        return result.getOrThrow()
    }

    /**
     * Explicitly logs a [Throwable]. The behaviour is the same as simply throwing it,
     * which is covered by [logResult] with a failed [Result].
     */
    fun logException(block: () -> Throwable): Unit = block().let {
        logResult { Result.failure(it) }
    }

    /**
     * Logs a caught [Throwable]. In contrast to [logResult] with a failed [Result] and [logException]
     * this method only marks the current logging context as failed but does not escalate (rethrow).
     */
    fun <R : Throwable> logCaughtException(block: () -> R): Unit = block().let { ex ->
        recoveredLoggers.add(this)
        render(true) { formatResult(Result.failure<R>(ex)) }
    }

    companion object {
        val DEFAULT: RenderingLogger = object : RenderingLogger {
            override fun render(trailingNewline: Boolean, block: () -> CharSequence) = block().let { TermUi.echo(it, trailingNewline) }
        }

        val recoveredLoggers = mutableListOf<RenderingLogger>()

        fun RenderingLogger.formatResult(result: Result<*>): CharSequence =
            if (result.isSuccess) formatReturnValue(result.toSingleLineString()) else formatException(" ", result.toSingleLineString())

        fun RenderingLogger.formatReturnValue(oneLiner: CharSequence): CharSequence {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.green
            val symbol = if (recoveredLoggers.contains(this)) heavyBallotX else heavyCheckMark
            return if (oneLiner.isEmpty()) format("$symbol") else format("$symbol") + " returned".italic() + " $oneLiner"
        }

        fun RenderingLogger.formatException(prefix: CharSequence, oneLiner: CharSequence?): CharSequence {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.red
            return oneLiner?.let {
                val event = if (recoveredLoggers.contains(this)) "recovered from" else "failed with"
                format("$greekSmallLetterKoppa") + prefix + "$event ${it.red()}"
            } ?: format("$greekSmallLetterKoppa")
        }
    }
}

inline fun <reified R, reified L : RenderingLogger> L.applyLogging(crossinline block: L.() -> R) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    logResult { runCatching { block() } }
}

inline fun <reified R, reified L : RenderingLogger> L.runLogging(crossinline block: L.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return logResult { runCatching { block() } }
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [singleLineLogging] is more suitable.
 */
inline fun <reified R> Any?.logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    borderedOutput: Boolean = (this as? BlockRenderingLogger)?.borderedOutput ?: false,
    block: BlockRenderingLogger.() -> R,
): R {
    val logger: BlockRenderingLogger = when (this) {
        is MutedRenderingLogger -> this
        is BlockRenderingLogger -> BlockRenderingLogger(
            caption = caption,
            borderedOutput = borderedOutput,
            statusInformationColumn = statusInformationColumn - prefix.length,
            statusInformationPadding = statusInformationPadding,
            statusInformationColumns = statusInformationColumns - prefix.length,
        ) { output -> logText { ansiCode.invoke(output) } }
        is RenderingLogger -> BlockRenderingLogger(caption = caption, borderedOutput = borderedOutput) { output -> logText { ansiCode.invoke(output) } }
        else -> BlockRenderingLogger(caption = caption, borderedOutput = borderedOutput)
    }
    val result: Result<R> = kotlin.runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}

/**
 * Creates a logger which logs to [path].
 */
inline fun <reified R> RenderingLogger?.fileLogging(
    path: Path,
    caption: CharSequence,
    block: RenderingLogger.() -> R,
): R = logging(caption) {
    logLine { "This process might produce pretty much log messages. Logging to …" }
    logLine { "${Unicode.Emojis.pageFacingUp} ${path.toUri()}" }
    val writer = path.bufferedWriter(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    val logger: RenderingLogger = BlockRenderingLogger(
        caption = caption,
        borderedOutput = false,
        log = { output: String ->
            writer.appendLine(output.removeEscapeSequences())
        },
    )
    kotlin.runCatching { block(logger) }.also { logger.logResult { it }; writer.close() }.getOrThrow()
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [logging] is more suitable.
 */
inline fun <reified R> BlockRenderingLogger?.singleLineLogging(
    caption: CharSequence,
    noinline block: SingleLineLogger.() -> R,
): R {
    val logger = object : SingleLineLogger(caption) {
        override fun render(block: () -> CharSequence) {
            this@singleLineLogging?.apply { logLine(block) } ?: TermUi.echo(block())
        }
    }
    return kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}

/**
 * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
 *
 * This logger logs all events using only a couple of characters. If more room is needed [singleLineLogging] or even [logging] is more suitable.
 */
inline fun <reified R> RenderingLogger?.microLogging(
    symbol: Grapheme,
    noinline block: MicroLogger.() -> R,
): R = if (this == null) {
    val logger: MicroLogger =
        object : MicroLogger(symbol) {
            override fun render(block: () -> CharSequence) {
            }
        }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
} else {
    val logger: MicroLogger = object : MicroLogger(symbol) {
        override fun render(block: () -> CharSequence) {
            this@microLogging.logLine(block)
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}

/**
 * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
 *
 * This logger logs all events using only a couple of characters. If more room is needed [singleLineLogging] or even [logging] is more suitable.
 */
inline fun <reified R> SingleLineLogger.microLogging(
    noinline block: MicroLogger.() -> R,
): R = run {
    val logger: MicroLogger = object : MicroLogger() {
        override fun render(block: () -> CharSequence) {
            this@microLogging.logLine(block)
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}
