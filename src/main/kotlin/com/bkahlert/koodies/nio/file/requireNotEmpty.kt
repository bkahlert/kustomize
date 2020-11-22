package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.exception.noSuchFile
import com.imgcstmzr.util.isDirectory
import java.nio.file.Path

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is empty, that is, has zero size
 * - this directory is empty, that is, has no entries
 */
fun Path.requireNotEmpty() {
    if (isEmpty) {
        if (isDirectory) throw noSuchFile(this, "Directory must not be empty but has no entries.")
        throw noSuchFile(this, "File must not be empty but has zero size.")
    }
}
