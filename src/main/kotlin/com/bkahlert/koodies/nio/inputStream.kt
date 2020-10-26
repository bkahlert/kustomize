package com.bkahlert.koodies.nio

import java.io.FileInputStream
import java.nio.file.Path

/**
 * Constructs a new [FileInputStream] of this file and returns it as a result.
 */
fun Path.inputStream(): FileInputStream = toFile().inputStream()
