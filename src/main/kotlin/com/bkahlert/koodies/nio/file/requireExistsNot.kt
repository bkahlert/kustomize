package com.bkahlert.koodies.nio.file

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path

/**
 * Throws if this [Path] does exist.
 */
fun Path.requireExistsNot() {
    if (exists) throw FileAlreadyExistsException(serialized)
}
