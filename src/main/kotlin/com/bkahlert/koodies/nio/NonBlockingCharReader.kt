package com.bkahlert.koodies.nio

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.string.random
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.singleLineLogger
import com.imgcstmzr.util.debug
import org.apache.commons.io.output.ByteArrayOutputStream
import org.jline.utils.NonBlocking
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

    val timeoutMillis = timeout.toLongMilliseconds()
    inline val inlineTimeoutMillis get() = timeoutMillis

//    private val reader: org.jline.utils.NonBlockingReader =
//        NonBlocking.nonBlocking(name, BufferedReader(org.jline.utils.InputStreamReader(inputStream, charset)))

    var reader: org.jline.utils.NonBlockingReader? = NonBlocking.nonBlocking(name, inputStream, charset)

    inline fun <reified T> read(buffer: CharArray, off: Int, logger: RenderingLogger<T>): Int = if (reader == null) -1 else
        logger.singleLineLogger(NonBlockingCharReader::class.simpleName + ".read(CharArray, Int, Int, Logger)") {
            when (val read = kotlin.runCatching { reader?.read(inlineTimeoutMillis) ?: throw IOException("No reader. Likely already closed.") }
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
                    buffer[off] = read.toChar()
                    1
                }
            }
        }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int = read(cbuf, off, MutedBlockRenderingLogger<Any?>())

    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }

    override fun toString(): String = "NonBlockingCharReader(inputStream=$inputStream, timeout=$timeout, charset=$charset, reader=$reader)"
}
