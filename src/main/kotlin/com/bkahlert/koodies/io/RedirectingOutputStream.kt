package com.bkahlert.koodies.io

import java.io.OutputStream

/**
 * An [OutputStream] that redirects everything written to it to the [redirection].
 */
class RedirectingOutputStream(private val redirection: (ByteArray) -> Unit) : OutputStream() {

    override fun write(b: Int) = write(byteArrayOf((b and 0xFF).toByte()))

    override fun write(b: ByteArray, off: Int, len: Int) {
        redirection.invoke(b.copyOfRange(off, off + len))
    }
}