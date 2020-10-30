package com.bkahlert.koodies.nio

import com.bkahlert.koodies.boolean.emoji
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.CR
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.LineSeparators.hasTrailingLineSeparator
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.time.Now
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.runtime.log.segment
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.quoted
import java.io.BufferedReader
import java.io.InputStream
import java.io.Reader
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * Non-blocking [Reader] with Unicode code support which is suitable to
 * process outputs interactively. That is, prompts that don't have a trailing
 * [LineSeparators] will also be read by [readLine], [readLines] and usual
 * helper functions.
 */
@OptIn(ExperimentalTime::class)
class NonBlockingReader(
    inputStream: InputStream,
    private val timeout: Duration = 6.seconds,
    private val logger: BlockRenderingLogger<String?>? = MutedBlockRenderingLogger("NonBlockingReader"),
    private val blockOnEmptyLine: Boolean = false,
) : BufferedReader(Reader.nullReader()) {
    private var reader: NonBlockingCharReader? = NonBlockingCharReader(inputStream, timeout = timeout / 3)
    private var lastReadLine: String? = null
    private var lastReadLineDueTimeout: Boolean? = null
    private var unfinishedLine: StringBuilder = StringBuilder()
    private var charArray: CharArray = CharArray(1)
    private val lastRead get() = unfinishedLine.substring((unfinishedLine.length - 1).coerceAtLeast(0), unfinishedLine.length)
    private val lastReadCR get() = lastRead == CR
    private val lastReadLF get() = lastRead == LF
    private val linePotentiallyComplete get() = unfinishedLine.matches(LineSeparators.INTERMEDIARY_LINE_PATTERN)
    private val justRead get() = String(charArray, 0, 1)
    private val justReadCR get() = justRead == CR
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
        logger.segment(NonBlockingReader::class.simpleName + "." + ::readLine.name + "()", ansiCode = ANSI.termColors.cyan) {
            val maxTimeMillis = System.currentTimeMillis() + timeout.toLongMilliseconds()
            logStatus { META typed "Starting to read line for at most $timeout" }
            while (true) {
                val read: Int = reader?.read(charArray, 0, 1, this@segment)!!

                if (read == -1) {
                    logStatus { META typed "InputStream Depleted. Closing. Unfinished Line: ${unfinishedLine.quoted}" }
                    close()
                    return@segment if (unfinishedLine.isEmpty()) {
                        lastReadLineDueTimeout = false
                        lastReadLine = null
                        lastReadLine
                    } else {
                        lastReadLineDueTimeout = false
                        lastReadLine = unfinishedLine.toString()
                        unfinishedLine.clear()
                        lastReadLine!!.withoutTrailingLineSeparator
                    }
                }
                logStatus { META typed Now.emoji + " ${(maxTimeMillis - System.currentTimeMillis()).milliseconds}; 📋 ${unfinishedLine.debug}; 🆕 ${justRead.debug}" }
                if (read == 1) {
//                    println(this@NonBlockingReader)
                    val lineAlreadyRead = lastReadLineDueTimeout == true && lastReadLine?.hasTrailingLineSeparator == true && !justReadCRLF

                    if (lineComplete) {
                        lastReadLineDueTimeout = false
                        lastReadLine = unfinishedLine.toString()
                        unfinishedLine.clear()
                        unfinishedLine.append(charArray)
                        logStatus { META typed "Line Completed: ${lastReadLine.quoted}" }
                        if (!lineAlreadyRead) {
                            return@segment lastReadLine!!.withoutTrailingLineSeparator
                        }
                    }
                    if (!lineAlreadyRead) {
                        unfinishedLine.append(charArray)
                    }
                }
                if (System.currentTimeMillis() >= maxTimeMillis && !(blockOnEmptyLine && unfinishedLine.isEmpty())) {
                    logStatus { META typed Now.emoji + " Timed out. Returning ${unfinishedLine.quoted}" }
                    lastReadLineDueTimeout = true
                    lastReadLine = unfinishedLine.toString()
                    return@segment lastReadLine!!.withoutTrailingLineSeparator
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
    fun forEachLine(block: (String) -> Unit): Unit = logger.miniSegment(NonBlockingReader::class.simpleName + "." + ::readLine.name + "()") {
        while (true) {
            val readLine: String? = readLine()
            val line = readLine ?: break
            block(line)
            logLine { "Finished processing $line" }
        }
    }

    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }

    override fun toString(): String = listOf(
        "unfinishedLine" to unfinishedLine.debug,
        "complete?" to "${linePotentiallyComplete.debug}/${lineComplete.debug}",
        "lastRead" to "${lastRead.debug} (␍? ${(lastRead == CR).emoji})",
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

