package com.bkahlert.koodies.io

import java.io.BufferedOutputStream
import java.io.File

/**
 * Constructs a buffered output stream wrapping a new [FileOutputStream] of this file and returns it as a result.
 */
fun File.bufferedOutputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream = outputStream().buffered(bufferSize)
