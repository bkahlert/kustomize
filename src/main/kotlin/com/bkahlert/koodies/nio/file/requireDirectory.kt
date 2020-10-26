package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.isDirectory
import java.nio.file.NotDirectoryException
import java.nio.file.Path

/**
 * Requires this to be a directory and throws an [IllegalArgumentException] otherwise.
 */
fun Path.requireDirectory() {
    if (!isDirectory) throw NotDirectoryException("$this")
}
