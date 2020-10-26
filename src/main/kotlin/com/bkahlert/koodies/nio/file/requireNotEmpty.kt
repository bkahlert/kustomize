package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.isDirectory
import java.nio.file.Path

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is empty, that is, has zero size
 * - this directory is empty, that is, has no entries
 */
fun Path.requireNotEmpty() {
    require(isNotEmpty) {
        if (isDirectory) "$this must not be empty but has no entries."
        else "$this must not be empty but has zero size."
    }
}
