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
import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.string.prefixWith
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import com.bkahlert.koodies.terminal.ansi.AnsiString.Companion.asAnsiString
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
import com.imgcstmzr.runtime.HasStatus
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.bufferedWriter

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
interface RenderingLogger<R> { // TODO remove R?

    /**
     * Prefix used to signify a nested logger.
     */
    val nestingPrefix: String get() = " :"

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
     * Logs the result of the process this logger is used for.
     */
    fun logResult(block: () -> Result<R>): R {
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
    fun logCaughtException(block: () -> Throwable): Unit = block().let { ex ->
        recoveredLoggers.add(this)
        render(true) { formatResult(Result.failure<R>(ex)) }
    }

    companion object {
        val DEFAULT: RenderingLogger<Any> = object : RenderingLogger<Any> {
            override fun render(trailingNewline: Boolean, block: () -> CharSequence) = block().let { TermUi.echo(it, trailingNewline) }
        }

        val recoveredLoggers = mutableListOf<RenderingLogger<*>>()

        fun RenderingLogger<*>.formatResult(result: Result<*>): CharSequence =
            if (result.isSuccess) formatReturnValue(result.toSingleLineString()) else formatException(" ", result.toSingleLineString())

        fun RenderingLogger<*>.formatReturnValue(oneLiner: CharSequence): CharSequence {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.green
            val symbol = if (recoveredLoggers.contains(this)) heavyBallotX else heavyCheckMark
            return if (oneLiner.isEmpty()) format("$symbol") else format("$symbol") + " returned".italic() + " $oneLiner"
        }

        fun RenderingLogger<*>.formatException(prefix: CharSequence, oneLiner: CharSequence?): CharSequence {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.red
            return oneLiner?.let {
                val event = if (recoveredLoggers.contains(this)) "recovered from" else "failed with"
                format("$greekSmallLetterKoppa") + prefix + "$event ${it.red()}"
            } ?: format("$greekSmallLetterKoppa")
        }
    }
}

inline fun <reified R> RenderingLogger<R>.applyLogging(crossinline block: RenderingLogger<R>.() -> R) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    logResult { runCatching { block() } }
}

inline fun <reified R> RenderingLogger<R>.runLogging(crossinline block: RenderingLogger<R>.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return logResult { runCatching { block() } }
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [singleLineLogger] is more suitable.
 */
inline fun <reified R> RenderingLogger<*>?.subLogger(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    borderedOutput: Boolean = (this as? BlockRenderingLogger<*>)?.borderedOutput ?: false,
    block: RenderingLogger<R>.() -> R,
): R {
    val logger: RenderingLogger<R> =
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
                BlockRenderingLogger(
                    caption = caption,
                    borderedOutput = borderedOutput,
                ) { output ->
                    val indentedOutput = output.asAnsiString().mapLines {
                        val prefixLength = "5"
                        val truncateBy = ansiCode.invoke(it.replaceFirst(("\\s{$prefixLength}").toRegex(), ' '.repeat(5 - prefix.length)))
                        truncateBy.prefixWith(prefix)
                    }
                    logText { indentedOutput }
                }
            }
        }
    return kotlin.runCatching { block(logger) }.also { logger.logResult { it } }.getOrThrow()
}


/**
 * Creates a logger which logs to [path].
 */
inline fun <reified R> RenderingLogger<*>?.fileLogger(
    path: Path,
    caption: CharSequence,
    block: RenderingLogger<R>.() -> R,
): R = subLogger(caption) {
    logLine { "This process might produce pretty much log messages. Logging to …" }
    logLine { "${Unicode.Emojis.pageFacingUp} ${path.toUri()}" }
    val writer = path.bufferedWriter(options = arrayOf(CREATE, TRUNCATE_EXISTING, WRITE))
    val logger: RenderingLogger<R> = BlockRenderingLogger(
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
 * This logger logs all events using a single line of text. If more room is needed [subLogger] is more suitable.
 */
inline fun <reified R> RenderingLogger<*>?.singleLineLogger(
    caption: CharSequence,
    noinline block: SingleLineLogger<R>.() -> R,
): R = if (this == null) {
    val logger: SingleLineLogger<R> =
        object : SingleLineLogger<R>(caption) {
            override fun render(block: () -> CharSequence) {
            }
        }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
} else {
    val logger: SingleLineLogger<R> = object : SingleLineLogger<R>(caption) {
        override fun render(block: () -> CharSequence) {
            val message = block()
            val prefix = this@singleLineLogger.nestingPrefix
            val logMessage = prefix + message.bold()
            this@singleLineLogger.render(true) { logMessage }
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}

/**
 * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
 *
 * This logger logs all events using only a couple of characters. If more room is needed [singleLineLogger] or even [subLogger] is more suitable.
 */
inline fun <reified R> RenderingLogger<*>?.microLog(
    symbol: Grapheme,
    noinline block: MicroLogger<R>.() -> R,
): R = if (this == null) {
    val logger: MicroLogger<R> =
        object : MicroLogger<R>(symbol) {
            override fun render(block: () -> CharSequence) {
            }
        }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
} else {
    val logger: MicroLogger<R> = object : MicroLogger<R>(symbol) {
        override fun render(block: () -> CharSequence) {
            val message = block()
            val prefix = this@microLog.nestingPrefix
            val logMessage = prefix + message.bold()
            this@microLog.render(true) { logMessage }
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}

/**
 * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
 *
 * This logger logs all events using only a couple of characters. If more room is needed [singleLineLogger] or even [subLogger] is more suitable.
 */
inline fun <reified R> RenderingLogger<*>?.microLog(
    noinline block: MicroLogger<R>.() -> R,
): R = if (this == null) {
    val logger: MicroLogger<R> =
        object : MicroLogger<R>() {
            override fun render(block: () -> CharSequence) {
            }
        }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
} else {
    val logger: MicroLogger<R> = object : MicroLogger<R>() {
        override fun render(block: () -> CharSequence) {
            val message = block()
            val prefix = this@microLog.nestingPrefix
            val logMessage = prefix + message.bold()
            this@microLog.render(true) { logMessage }
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}
