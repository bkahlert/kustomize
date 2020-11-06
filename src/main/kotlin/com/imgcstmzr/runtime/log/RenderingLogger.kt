package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.nullable.invoke
import com.bkahlert.koodies.string.Unicode.Emojis.heavyBallotX
import com.bkahlert.koodies.string.Unicode.Emojis.heavyCheckMark
import com.bkahlert.koodies.string.Unicode.greekSmallLetterKoppa
import com.bkahlert.koodies.string.prefixWith
import com.bkahlert.koodies.string.truncateBy
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareMapLines
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.mordant.AnsiCode
import com.imgcstmzr.runtime.HasStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
interface RenderingLogger<R> {

    val nestingPrefix: String get() = " :"

    /**
     * Method that is responsible to render what gets logged.
     *
     * All default implemented methods use this method.
     */
    fun render(trailingNewline: Boolean, block: () -> String)

    fun logText(block: () -> String): RenderingLogger<R> = block().let { output ->
        render(false) { output }
        this
    }

    fun logLine(block: () -> String): RenderingLogger<R> = block().let { output ->
        render(true) { output }
        this
    }

    fun logStatus(items: List<HasStatus> = emptyList(), block: () -> IO = { OUT typed "" }): RenderingLogger<R> = block().let { output ->
        render(true) { output.formatted + " (${items.size})" }
        this
    }

    fun logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) { formatResult(result) }
        return result.getOrThrow()
    }

    /**
     * Explicitly logs a [Throwable]. The behaviour is the same as simply throwing it,
     * which is covered by [logResult] with a failed [Result].
     */
    fun logException(block: () -> Throwable): RenderingLogger<R> = block().let {
        logResult { Result.failure(it) }
        this
    }

    /**
     * Logs a caught [Throwable]. In contrast to [logResult] with a failed [Result] and [logException]
     * this method only marks the current logging context as failed but does not escalate (rethrow).
     */
    fun logCaughtException(block: () -> Throwable): RenderingLogger<R> = block().let { ex ->
        recoveredLoggers.add(this)
        render(true) { formatResult(Result.failure<R>(ex)) }
        this
    }

    companion object {
        val DEFAULT: RenderingLogger<Any> = object : RenderingLogger<Any> {
            override fun render(trailingNewline: Boolean, block: () -> String) = block().let { TermUi.echo(it, trailingNewline) }
        }

        val recoveredLoggers = mutableListOf<RenderingLogger<*>>()

        fun RenderingLogger<*>.formatResult(result: Result<*>): String =
            if (result.isSuccess) formatReturnValue(result.toSingleLineString()) else formatException(" ", result.toSingleLineString())

        fun RenderingLogger<*>.formatReturnValue(oneLiner: String): String {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.green
            val symbol = if (recoveredLoggers.contains(this)) heavyBallotX else heavyCheckMark
            return if (oneLiner.isEmpty()) format("$symbol") else format("$symbol") + " returned".italic() + " $oneLiner"
        }

        fun RenderingLogger<*>.formatException(prefix: String, oneLiner: String?): String {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.red
            return oneLiner?.let {
                val event = if (recoveredLoggers.contains(this)) "recovered from" else "failed with"
                format("$greekSmallLetterKoppa") + prefix + "$event ${it.red()}"
            } ?: format("$greekSmallLetterKoppa")
        }

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
                        val color = ansiCode
                        BlockRenderingLogger(
                            caption = caption,
                            borderedOutput = borderedOutput,
                        ) { output ->
                            val indentedOutput = output.ansiAwareMapLines {
                                val truncateBy = color.invoke(it.truncateBy(prefix.length, minWhitespaceLength = 3))
                                truncateBy.prefixWith(prefix)
                            }
                            logText { indentedOutput }
                        }
                    }
                }
            return kotlin.runCatching { block(logger) }.also { logger.logResult { it } }.getOrThrow()
        }

        @JvmName("simpleSubLogger")
        inline fun <R> RenderingLogger<R>?.subLogger(
            caption: String,
            ansiCode: AnsiCode? = null,
            borderedOutput: Boolean = (this as? BlockRenderingLogger)?.borderedOutput ?: false,
            block: RenderingLogger<R>.() -> R,
        ): R = subLogger<R, R>(caption, ansiCode, borderedOutput, block)

        inline fun <reified R1, reified R2> RenderingLogger<R1>?.singleLineLogger(
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
                    val prefix = this@singleLineLogger.nestingPrefix
                    val logMessage = prefix + message.bold()
                    this@singleLineLogger.render(true) { logMessage }
                }
            }
            kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
        }

        @JvmName("simpleSingleLineLogger")
        inline fun <reified R> RenderingLogger<R>?.singleLineLogger(
            caption: String,
            noinline block: SingleLineLogger<R>.() -> R,
        ): R = singleLineLogger<R, R>(caption, block)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T : RenderingLogger<R>, R> T.applyLogging(crossinline block: T.() -> R) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    logResult { runCatching { block() } }
}

