package com.imgcstmzr.util.logging

import com.imgcstmzr.util.logging.NegativeIndexSupportList.Companion.negativeIndexSupport
import com.imgcstmzr.util.logging.OutputCapture.Companion.splitOutput
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Allows to capture a pair of output and error stream like [System.out] and [System.err]
 * without applying any modifications - namely ANSI escaping removal - as does Spring Boot's `OutputCapture`.
 */
class AdHocOutputCapture : CapturedOutput {
    private val allStream = ByteArrayOutputStream()
    private val outStream = ByteArrayOutputStream()
    private val errStream = ByteArrayOutputStream()
    private var oldOut: PrintStream? = null
    private var oldErr: PrintStream? = null

    private fun startCapturing(out: PrintStream, err: PrintStream) {
        oldOut = out
        oldErr = err
        System.setOut(TeePrintStream(out, outStream, allStream))
        System.setErr(TeePrintStream(err, errStream, allStream))
    }

    private fun stopCapturing() {
        System.setOut(oldOut)
        System.setErr(oldErr)
    }

    fun runCapturing(runnable: () -> Unit) {
        startCapturing(System.out, System.err)
        try {
            runnable()
        } finally {
            stopCapturing()
        }
    }

    override val all: String get() = allStream.toString()
    override val allLines: List<String> by negativeIndexSupport { splitOutput(all) }

    override val out: String get() = outStream.toString()
    override val outLines: List<String> by negativeIndexSupport { splitOutput(out) }

    override val err: String get() = errStream.toString()
    override val errLines: List<String> by negativeIndexSupport { splitOutput(err) }

    /**
     * Print stream that forwards all calls to a [TeeOutputStream] of the given output streams
     */
    private class TeePrintStream(vararg streams: OutputStream) :
        PrintStream(TeeOutputStream(*streams), false, StandardCharsets.UTF_8.name())

    /**
     * Output stream that forwards all output calls to the given output streams
     */
    private class TeeOutputStream(vararg streams: OutputStream) : OutputStream() {
        private var streams: List<OutputStream> = streams.toList()
        override fun write(byte_: Int) = streams.forEach { it.write(byte_) }
        override fun write(buffer: ByteArray) = streams.forEach { it.write(buffer) }
        override fun write(buf: ByteArray, off: Int, len: Int) = streams.forEach { it.write(buf, off, len) }
        override fun flush() = streams.forEach { it.flush() }
        override fun close() = streams.forEach { it.close() }
    }

    companion object {
        /**
         * Runs the executable with [System.out] and [System.err] redirected
         * and provides access to the redirected output through [CapturedOutput].
         *
         * @param runnable the executable of which the produced output get captured
         *
         * @return the captured output
         */
        fun capture(runnable: () -> Unit): CapturedOutput {
            val capture = AdHocOutputCapture()
            capture.runCapturing(runnable)
            return capture
        }
    }


    override val length: Int get() = all.length
    override fun get(index: Int): Char = all[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = all.subSequence(startIndex, endIndex)
    override fun toString(): String = all
}
