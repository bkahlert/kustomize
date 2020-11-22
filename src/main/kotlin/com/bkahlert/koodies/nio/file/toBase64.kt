package com.bkahlert.koodies.nio.file

import java.nio.file.Path
import java.util.Base64

/**
 * Converts this path using Base64.
 */
fun Path.toBase64(): String = Base64.getEncoder().encodeToString(readBytes())
