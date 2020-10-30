package com.bkahlert.koodies.nio

import com.imgcstmzr.util.resourceAsStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path

/**
 * Constructs a new [FileInputStream] of this file and returns it as a result.
 */
fun Path.inputStream(): InputStream = resourceAsStream() ?: error("Error opening $this")
