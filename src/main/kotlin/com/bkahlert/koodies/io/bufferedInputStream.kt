package com.bkahlert.koodies.io

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

/**
 * Constructs a buffered input stream wrapping a new [FileInputStream] of this file and returns it as a result.
 */
fun File.bufferedInputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream = inputStream().buffered(bufferSize)

