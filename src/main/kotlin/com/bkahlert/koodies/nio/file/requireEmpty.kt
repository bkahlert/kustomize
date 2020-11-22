package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.exception.directoryNotEmpty
import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.util.isDirectory
import java.nio.file.Path

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is not empty, that is, has size greater zero
 * - this directory is not empty, that is, has entries
 */
fun Path.requireEmpty() {
    if (isNotEmpty) {
        if (isDirectory) throw directoryNotEmpty(this)
        throw fileAlreadyExists(this, "Must be empty but has $size.")
    }
}
