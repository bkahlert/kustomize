package com.bkahlert.koodies.nio

import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.Quiet
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.runtime.log.segment
import com.imgcstmzr.util.Now
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.quoted
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class NonBlockingReader(
    inputStream: InputStream,
    private val timeoutMillis: Long = 6000,
    private val logger: BlockRenderingLogger<String?, HasStatus>? = BlockRenderingLogger.Quiet(),
) {
    @ExperimentalTime constructor(
        inputStream: InputStream,
        timeout: Duration,
        logger: BlockRenderingLogger<String?, HasStatus>? = BlockRenderingLogger.Quiet(),
    ) : this(inputStream, timeout.toLongMilliseconds(), logger)

    private val partialLineReader: PartialLineReader = PartialLineReader(inputStream.convert())
    private var lastRead: String = ""

    @OptIn(ExperimentalTime::class)
    fun readLine(): String? = logger.segment(NonBlockingReader::class.simpleName + "." + ::readLine.name + "()", ansiCode = termColors.cyan) {
        var line: String?
        val maxTimeMillis = System.currentTimeMillis() + timeoutMillis
        while (true) {
            logLine(OUT typed "Waiting for max ${timeoutMillis.milliseconds}")
            line = partialLineReader.readPartialLine(this@segment) ?: return@segment null
            Now.emoji + " Timeout: ${(maxTimeMillis - System.currentTimeMillis()).milliseconds}; read last time: ${lastRead.debug}; read: ${line.debug}"
            if (line === lastRead) {
                if (System.currentTimeMillis() >= maxTimeMillis) {
                    Now.emoji + " Timed out. Returning ${line.quoted}"
                    return@readLine line
                }
                val pause: Duration = 100.milliseconds
                Now.emoji + " Nothing new read. Waiting $pause"
                Thread.sleep(pause.toLongMilliseconds())
            } else {
                lastRead = line
                "Returning ${line.quoted}"
                return@readLine line
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("return statement missing")
    }

    fun forEachLine(block: (String) -> Unit): Unit = logger.miniSegment(NonBlockingReader::class.simpleName + "." + ::readLine.name + "()") {
        while (true) {
            val line = readLine() ?: break
            block(line)
            log("Finished processing $line", true)
        }
    }
}

