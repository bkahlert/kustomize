package com.bkahlert.koodies.nio

import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.bkahlert.koodies.terminal.ansi.Style.Companion.gray
import com.bkahlert.koodies.terminal.ansi.Style.Companion.strikethrough
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.segment
import com.imgcstmzr.util.debug
import java.io.ByteArrayOutputStream

/**
 * [ByteArrayOutputStream] that can be consumed by acknowledging
 * that number of bytes accepted whereas the unaccepted rest
 * will not be marked as consumed and therefore will constitute the start
 * of the return string on the next [ConsumableByteArrayOutputStream.readAll]
 * call.
 *
 * This will repeat itself until the stream is closed.
 */
class ConsumableByteArrayOutputStream() : ByteArrayOutputStream() {

    private var isClosed: Boolean = false
    override fun close() {
        isClosed = true
    }

    private var offset: Int = 0
    private var lastRead: Pair<IntRange, String> = 0..0 to ""

    fun readAll(logger: BlockRenderingLogger<String?, HasStatus>? = null): String? =
        logger.segment(ConsumableByteArrayOutputStream::class.simpleName + "." + ::readAll.name + "()", ansiCode = termColors.brightBlue) {
            return@readAll toByteArray().let { buffer ->
                logLine(OUT typed writtenBuf().debug)
                if (offset == buffer.size && isClosed) {
                    logLast(Result.success(null))
                    null
                } else {
                    if (offset..buffer.size == lastRead.first) lastRead.second
                        .also {
                            logLast(Result.success("repeating output: $it"))
                        }
                    else String(buffer, offset, (buffer.size - offset))
                        .also { read ->
                            logLast(Result.success("new output: $read"))
                            lastRead = offset..buffer.size to read
                        }
                }
            }
        }

    fun ack(accepted: Int) {
        offset += accepted
        check(offset <= count)
    }

    val List<Byte>.debug
        get() = "$size/$count/${buf.size} — " + mapIndexed { index, byte: Byte ->
            if (index < offset) {
                byte.debug.gray().strikethrough()
            } else {
                byte.debug
            }
        }.joinToString(",")

    private fun writtenBuf() = buf.takeWhile { it != 0.toByte() }
}
