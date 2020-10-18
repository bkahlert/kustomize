package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.random
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.util.debug
import org.jline.utils.NonBlocking
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Reads a [ByteArrayOutputStream] line-wise using a special rule:
 * If no complete line is available the currently available content is returned.
 * This happens all over again until that line is complete, that is, terminated
 * by `\r\n` or `\n`.
 */
@ExperimentalTime
class NonBlockingCharReader(
    private val inputStream: InputStream,
    private val timeout: Duration = 6.seconds,
    private val charset: Charset = Charsets.UTF_8,
    name: String = "ImgCstmzr-${NonBlockingCharReader::class.simpleName}-${String.random()}",
) : Reader() {

//    private val reader: org.jline.utils.NonBlockingReader =
//        NonBlocking.nonBlocking(name, BufferedReader(org.jline.utils.InputStreamReader(inputStream, charset)))

    private var reader: org.jline.utils.NonBlockingReader? = NonBlocking.nonBlocking(name, inputStream, charset)

    fun read(cbuf: CharArray, off: Int, len: Int, logger: BlockRenderingLogger<String?>): Int = if (reader == null) -1 else
        logger.miniSegment(NonBlockingCharReader::class.simpleName + ".read(CharArray, Int, Int, Logger)") {
            when (val read = kotlin.runCatching { reader?.read(timeout.toLongMilliseconds()) ?: throw IOException("No reader. Likely already closed.") }
                .recover {
                    reader?.close()
                    -1
                }.getOrThrow()) {
                -1 -> {
                    logStatus { META typed "EOF" }
                    -1
                }
                -2 -> {
                    logStatus { META typed "TIMEOUT" }
                    0
                }
                else -> {
                    logStatus { META typed "SUCCESSFULLY READ ${read.debug}" }
                    cbuf[off] = read.toChar()
                    1
                }
            }
        }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int = read(cbuf, off, len, MutedBlockRenderingLogger())

    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }
}
