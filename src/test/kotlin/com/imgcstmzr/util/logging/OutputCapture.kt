package com.imgcstmzr.util.logging

import com.imgcstmzr.util.logging.NegativeIndexSupportList.Companion.negativeIndexSupport
import com.imgcstmzr.util.splitLineBreaks
import java.io.OutputStream
import java.io.PrintStream
import java.util.function.Consumer

internal class OutputCapture : CapturedOutput {
    companion object {
        fun splitOutput(output: String): List<String> = output.splitLineBreaks().dropLastWhile { it.isBlank() }
    }

    private val systemCaptures: ArrayDeque<SystemCapture> = ArrayDeque()
    fun push() = systemCaptures.addLast(SystemCapture())
    fun pop() = systemCaptures.removeLast().release()

    override val all: String get() = getFilteredCapture { type: Type? -> true }
    override val allLines: List<String> by negativeIndexSupport { splitOutput(all) }

    override val out: String get() = getFilteredCapture { other: Type? -> Type.OUT == other }
    override val outLines: List<String> by negativeIndexSupport { splitOutput(out) }

    override val err: String get() = getFilteredCapture { other: Type? -> Type.ERR == other }
    override val errLines: List<String> by negativeIndexSupport { splitOutput(err) }

    /**
     * Resets the current capture session, clearing its captured output.
     */
    fun reset() = systemCaptures.lastOrNull()?.reset()

    private fun getFilteredCapture(filter: (Type) -> Boolean): String {
        val builder = StringBuilder()
        for (systemCapture in systemCaptures) {
            systemCapture.append(builder, filter)
        }
        return builder.toString()
    }

    /**
     * Types of content that can be captured.
     */
    private enum class Type {
        OUT, ERR
    }

    /**
     * A capture session that captures [System.out] and [System.err].
     */
    private class SystemCapture() {
        private val monitor = Any()
        private val out: PrintStreamCapture
        private val err: PrintStreamCapture
        private val capturedStrings: MutableList<CapturedString> = ArrayList()
        fun release() {
            System.setOut(out.parent)
            System.setErr(err.parent)
        }

        private fun captureOut(string: String) {
            synchronized(monitor) { capturedStrings.add(CapturedString(Type.OUT, string)) }
        }

        private fun captureErr(string: String) {
            synchronized(monitor) { capturedStrings.add(CapturedString(Type.ERR, string)) }
        }

        fun append(builder: StringBuilder, filter: (Type) -> Boolean) {
            synchronized(monitor) {
                capturedStrings
                    .asSequence()
                    .filter { filter(it.type) }
                    .forEach { builder.append(it) }
            }
        }

        fun reset() {
            synchronized(monitor) { capturedStrings.clear() }
        }

        init {
            out = PrintStreamCapture(System.out) { string: String -> captureOut(string) }
            err = PrintStreamCapture(System.err) { string: String -> captureErr(string) }
            System.setOut(out)
            System.setErr(err)
        }

        private class CapturedString(val type: Type, private val string: String) {
            override fun toString(): String = string
        }
    }

    /**
     * A [PrintStream] implementation that captures written strings.
     */
    private class PrintStreamCapture(val parent: PrintStream, copy: Consumer<String>) :
        PrintStream(OutputStreamCapture(getSystemStream(parent), copy)) {

        companion object {
            private fun getSystemStream(printStream: PrintStream): PrintStream {
                var systemStream = printStream
                while (systemStream is PrintStreamCapture) systemStream = systemStream.parent
                return systemStream
            }
        }
    }

    /**
     * An [OutputStream] implementation that captures written strings.
     */
    private class OutputStreamCapture(private val systemStream: PrintStream, private val copy: Consumer<String>) : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf((b and 0xFF).toByte()))

        override fun write(b: ByteArray, off: Int, len: Int) {
            copy.accept(String(b, off, len))
            systemStream.write(b, off, len)
        }

        override fun flush() = systemStream.flush()
    }


    override fun hashCode(): Int = all.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        return if (other is CapturedOutput || other is CharSequence) {
            all == other.toString()
        } else false
    }

    override val length: Int get() = all.length
    override fun get(index: Int): Char = all[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = all.subSequence(startIndex, endIndex)
    override fun toString(): String = all
}
