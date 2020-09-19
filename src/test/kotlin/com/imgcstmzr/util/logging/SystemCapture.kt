package com.imgcstmzr.util.logging

/**
 * A capture session that captures [System.out] and [System.err].
 */
open class SystemCapture() {
    private val monitor = Any()
    private val out: PrintStreamCapture
    private val err: PrintStreamCapture
    private val capturedStrings: MutableList<CapturedString> = ArrayList()
    fun release() {
        System.setOut(out.parent)
        System.setErr(err.parent)
    }

    private fun captureOut(string: String) {
        synchronized(monitor) { capturedStrings.add(CapturedString(OutputCapture.Type.OUT, string)) }
    }

    private fun captureErr(string: String) {
        synchronized(monitor) { capturedStrings.add(CapturedString(OutputCapture.Type.ERR, string)) }
    }

    internal fun append(builder: StringBuilder, filter: (OutputCapture.Type) -> Boolean) {
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

    private class CapturedString(val type: OutputCapture.Type, private val string: String) {
        override fun toString(): String = string
    }
}
