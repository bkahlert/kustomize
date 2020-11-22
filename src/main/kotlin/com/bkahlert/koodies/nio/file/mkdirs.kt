package com.bkahlert.koodies.nio.file

import java.nio.file.Files
import java.nio.file.Path

/**
 * Creates a directory by creating all nonexistent parent directories first.
 *
 * @see [Files.createDirectories]
 */
fun Path.mkdirs(): Path = Files.createDirectories(this)
