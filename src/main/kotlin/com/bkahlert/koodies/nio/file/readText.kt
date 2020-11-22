package com.bkahlert.koodies.nio.file

import java.nio.charset.Charset
import java.nio.file.Path

/**
 * Gets the entire content of this file as a String using UTF-8 or specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
 *
 * @param charset character set to use.
 * @return the entire content of this file as a String.
 */
fun Path.readText(charset: Charset = Charsets.UTF_8): String =
    reader(charset).use { it.readText() }
