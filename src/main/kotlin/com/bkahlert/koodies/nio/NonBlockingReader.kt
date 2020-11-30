package com.bkahlert.koodies.nio

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.CR
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.LineSeparators.hasTrailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.time.Now
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.singleLineLogger
import com.imgcstmzr.runtime.log.subLogger
import com.imgcstmzr.util.debug
import java.io.BufferedReader
import java.io.InputStream
import java.io.Reader
import java.lang.System.currentTimeMillis
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * Non-blocking [Reader] with Unicode code support which is suitable to
 * process outputs interactively. That is, prompts that don't have a trailing
 * [LineSeparators] will also be read by [readLine], [readLines] and usual
 * helper functions.
 */
class NonBlockingReader(
    inputStream: InputStream,
    private val timeout: Duration = 6.seconds,
    private val logger: RenderingLogger<*> = MutedBlockRenderingLogger<Any>("NonBlockingReader"),
    private val blockOnEmptyLine: Boolean = false,
) : BufferedReader(Reader.nullReader()) {
    init {
        check(timeout.isPositive()) { "Timeout must greater 0" }
    }

    private var reader: NonBlockingCharReader? = NonBlockingCharReader(inputStream, timeout = timeout / 3)
    private var lastReadLine: String? = null
    private var lastReadLineDueTimeout: Boolean? = null
    private var unfinishedLine: StringBuilder = StringBuilder()
    private var charArray: CharArray = CharArray(1)
    private val lastRead get() = unfinishedLine.substring((unfinishedLine.length - 1).coerceAtLeast(0), unfinishedLine.length)
    private val lastReadCR get() = lastRead == CR
    private val linePotentiallyComplete get() = unfinishedLine.matches(LineSeparators.INTERMEDIARY_LINE_PATTERN)
    private val justRead get() = String(charArray, 0, 1)
    private val justReadLF get() = justRead == LF
    private val justReadCRLF get() = lastReadCR && justReadLF
    private val lineComplete get() = linePotentiallyComplete && !justReadCRLF

    /**
     * Reads the next line from the [InputStream].
     *
     * Should more time pass than [timeout] the unfinished line is returned but also kept
     * for the next attempt. The unfinished line will be completed until a line separator
     * or EOF was encountered.
     */
    override fun readLine(): String? = if (reader == null) null else
        logger.subLogger(NonBlockingReader::class.simpleName + "." + ::readLine.name + "()", ansiCode = ANSI.termColors.cyan) {
            val maxTimeMillis = currentTimeMillis() + timeout.toLongMilliseconds()
            logStatus { META typed "Starting to read line for at most $timeout" }
            while (true) {
                val read: Int = reader?.read(charArray, 0, this@subLogger)!!

                if (read == -1) {
                    logStatus { META typed "InputStream Depleted. Closing. Unfinished Line: ${unfinishedLine.quoted}" }
                    close()
                    return@subLogger if (unfinishedLine.isEmpty()) {
                        lastReadLineDueTimeout = false
                        lastReadLine = null
                        lastReadLine
                    } else {
                        lastReadLineDueTimeout = false
                        lastReadLine = "$unfinishedLine"
                        unfinishedLine.clear()
                        lastReadLine!!.withoutTrailingLineSeparator
                    }
                }
                logStatus { META typed "${Now.emoji} ${(maxTimeMillis - currentTimeMillis()).milliseconds}; 📋 ${unfinishedLine.debug}; 🆕 ${justRead.debug}" }
                if (read == 1) {
//                    println(this@NonBlockingReader)
                    val lineAlreadyRead = lastReadLineDueTimeout == true && lastReadLine?.hasTrailingLineSeparator == true && !justReadCRLF

                    if (lineComplete) {
                        lastReadLineDueTimeout = false
                        lastReadLine = "$unfinishedLine"
                        unfinishedLine.clear()
                        unfinishedLine.append(charArray)
                        logStatus { META typed "Line Completed: ${lastReadLine.quoted}" }
                        if (!lineAlreadyRead) {
                            return@subLogger lastReadLine!!.withoutTrailingLineSeparator
                        }
                    }
                    if (!lineAlreadyRead) {
                        unfinishedLine.append(charArray)
                    }
                }
                if (currentTimeMillis() >= maxTimeMillis && !(blockOnEmptyLine && unfinishedLine.isEmpty())) {
                    logStatus { META typed "${Now.emoji} Timed out. Returning ${unfinishedLine.quoted}" }
                    // TODO evaluate if better to call a callback and continue working (without returning half-read lines)
                    lastReadLineDueTimeout = true
                    lastReadLine = "$unfinishedLine"
                    return@subLogger lastReadLine!!.withoutTrailingLineSeparator
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("return statement missing")
        }

    /**
     * Reads all lines from the [InputStream].
     *
     * Should more time pass than [timeout] the unfinished line is returned but also kept
     * for the next attempt. The unfinished line will be completed until a line separator
     * or EOF was encountered.
     */
    fun forEachLine(block: (String) -> Unit): String? = logger.singleLineLogger(NonBlockingReader::class.simpleName + "." + ::forEachLine.name + "()") {
        var lineCount = 0
        while (true) {
            val readLine: String? = readLine()
            val line = readLine ?: break
            block(line)
            lineCount++
        }
        "$lineCount processed"
    }

    /**
     * Closes this reader without throwing any exception.
     */
    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }

    override fun toString(): String = listOf(
        "unfinishedLine" to unfinishedLine.debug,
        "complete?" to "${linePotentiallyComplete.debug}/${lineComplete.debug}",
        "lastRead" to "${lastRead.debug} (␍? ${(lastRead == CR).asEmoji})",
        "justRead" to "${justRead.debug} (␊? ${(justRead == LF).debug})",
        "␍␊?" to justReadCRLF.debug,
        "lastReadLine" to lastReadLine.debug,
        "lastReadLineDueTimeout?" to lastReadLineDueTimeout.debug,
        "timeout" to timeout,
        "logger" to logger,
        "reader" to "…",
    ).joinToString(prefix = "NonBlockingReader(",
        separator = "; ",
        postfix = ")") { "${it.first}: ${it.second}" }
}

