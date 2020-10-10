package com.bkahlert.koodies.nio

import com.bkahlert.koodies.thread.startAsDaemon
import java.io.InputStream

/**
 * Converts this [InputStream] into a [ConsumableByteArrayOutputStream] by copying the data of this
 * input stream in a separate thread into the output stream.
 */
fun InputStream.convert(): ConsumableByteArrayOutputStream =
    ConsumableByteArrayOutputStream().apply {
        startAsDaemon {
            this@convert.use { input ->
                this@apply.use { output ->
                    // WARNING: Some code depends on byte-wise copy.
                    // A couple of tests will break if you change this value.
                    copyTo(output, bufferSize = 1)
                }
            }
        }
    }

