package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.LineSeparators.firstLineSeparatorLength
import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.segment
import java.io.ByteArrayOutputStream

/**
 * Reads a [ByteArrayOutputStream] line-wise using a special rule:
 * If no complete line is available the currently available content is returned.
 * This happens all over again until that line is complete, that is, terminated
 * by `\r\n` or `\n`.
 */
class PartialLineReader(
    private val outputStream: ConsumableByteArrayOutputStream,
) {
    fun readPartialLine(logger: BlockRenderingLogger<String?>? = null): String? =
        logger.segment(PartialLineReader::class.simpleName + "." + ::readPartialLine.name + "()", ansiCode = ANSI.EscapeSequences.termColors.blue) {
            return@readPartialLine outputStream.readAll(this@segment)
                .let { content ->
                    if (content == null) {
                        logLast(Result.success(null))
                        null
                    } else {
                        val lines = content.lines()
                        if (lines.size == 1) content.also {
                            logLast(Result.success("rest of the line: $it"))
                        }
                        else lines.first().also {
                            logLast(Result.success("ready line: $it"))
                            outputStream.ack(it.length + content.take(it.length + 2).firstLineSeparatorLength)
                        }
                    }
                }
        }
}
