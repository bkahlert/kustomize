package com.bkahlert.koodies.nio

import java.io.FileOutputStream
import java.nio.file.Path

/**
 * Constructs a new [FileOutputStream] of this file and returns it as a result.
 */
fun Path.outputStream(): FileOutputStream = toFile().outputStream()
