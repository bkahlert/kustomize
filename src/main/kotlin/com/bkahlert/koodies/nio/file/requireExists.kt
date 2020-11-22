package com.bkahlert.koodies.nio.file

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path

/**
 * Throws if this [Path] does not exist.
 */
fun Path.requireExists() {
    if (notExists) throw FileAlreadyExistsException(serialized)
}
