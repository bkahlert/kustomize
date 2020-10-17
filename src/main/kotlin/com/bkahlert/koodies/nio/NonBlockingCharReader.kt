package com.bkahlert.koodies.nio

import com.bkahlert.koodies.terminal.ascii.Kaomojis.Dogs.random
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.util.debug
import org.jline.utils.NonBlocking
import org.jline.utils.NonBlockingReader
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Reads a [ByteArrayOutputStream] line-wise using a special rule:
 * If no complete line is available the currently available content is returned.
 * This happens all over again until that line is complete, that is, terminated
 * by `\r\n` or `\n`.
 */
class NonBlockingCharReader(private val reader: NonBlockingReader, private val timeout: Long = 6000) : Reader() {
    @ExperimentalTime constructor(
        inputStream: InputStream,
        timeout: Duration,
        charset: Charset = StandardCharsets.UTF_8,
        name: String = "ImgCstmzr-${NonBlockingCharReader::class.simpleName}-${random()}",
    ) : this(NonBlocking.nonBlocking(name, BufferedReader(InputStreamReader(inputStream, charset))), timeout.toLongMilliseconds())

    fun read(cbuf: CharArray, off: Int, len: Int, logger: BlockRenderingLogger<String?>): Int =
        logger.miniSegment<String?, Int>(NonBlockingCharReader::class.simpleName + ".read(CharArray, Int, Int, Logger)") {
            when (val read = reader.read(timeout)) {
                -1 -> {
                    logLineLambda { META typed "EOF" }
                    -1
                }
                -2 -> {
                    logLineLambda { META typed "TIMEOUT" }
                    0
                }
                else -> {
                    logLineLambda { META typed "SUCCESSFULLY READ ${read.debug}" }
                    cbuf[off] = read.toChar()
                    1
                }
            }
        }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        return read(cbuf, off, len, MutedBlockRenderingLogger())
    }

    override fun close() = reader.close()
}
