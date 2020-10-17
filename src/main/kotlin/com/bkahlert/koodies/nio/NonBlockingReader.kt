package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.runtime.log.segment
import com.imgcstmzr.util.Now
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
@ExperimentalTime
class NonBlockingReader(
    inputStream: InputStream,
    private val timeout: Duration = 6.seconds,
    private val logger: BlockRenderingLogger<String?>? = MutedBlockRenderingLogger("NonBlockingReader"),
) : BufferedReader(Reader.nullReader()) {
    private var reader: NonBlockingCharReader? = NonBlockingCharReader(inputStream, timeout = timeout / 3)
    private var unfinishedLine: StringBuilder = StringBuilder()
    private var charArray: CharArray = CharArray(1)

    /**
     * Reads the next line from the [InputStream].
     *
     * Should more time pass than [timeout] the unfinished line is returned but also kept
     * for the next attempt. The unfinished line will be completed until a line separator
     * or EOF was encountered.
     */
    override fun readLine(): String? = if (reader == null) null else
        logger.segment(NonBlockingReader::class.simpleName + "." + ::readLine.name + "()", ansiCode = termColors.cyan) {
            val maxTimeMillis = System.currentTimeMillis() + timeout.toLongMilliseconds()
            logLineLambda { META typed "Starting to read line for at most $timeout" }
            while (true) {
                val read: Int = reader?.read(charArray, 0, 1, this@segment)!!
                if (read == -1) {
                    logLineLambda { META typed "InputStream Depleted. Closing. Unfinished Line: ${unfinishedLine.quoted}" }
                    close()
                    return@segment null
                }
                logLineLambda { META typed Now.emoji + " ${(maxTimeMillis - System.currentTimeMillis()).milliseconds}; 📋 ${unfinishedLine.debug}" }
                if (read == 1) {
                    unfinishedLine.append(charArray)
                    if (unfinishedLine.matches(LineSeparators.INTERMEDIARY_LINE_PATTERN)) {
                        val line = unfinishedLine.withoutTrailingLineSeparator
                        unfinishedLine.clear()
                        logLineLambda { META typed "Line Completed: ${line.quoted}" }
                        return@segment line
                    }
                }
                if (System.currentTimeMillis() >= maxTimeMillis) {
                    logLineLambda { META typed Now.emoji + " Timed out. Returning ${unfinishedLine.quoted}" }
                    return@segment unfinishedLine.toString()
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
            logLambda(true) { "Finished processing $line" }
        }
    }

    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }

    override fun toString(): String =
        "NonBlockingReader(unfinishedLine=$unfinishedLine, charArray=${charArray.contentToString()}, timeout=$timeout, logger=$logger, reader=$reader)"
}

