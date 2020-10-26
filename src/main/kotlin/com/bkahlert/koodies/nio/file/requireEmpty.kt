package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.isDirectory
import java.nio.file.Path

/**
 * Depending on the file type throws [IllegalArgumentException] if
 * - this file is not empty, that is, has size greater zero
 * - this directory is not empty, that is, has entries
 */
fun Path.requireEmpty() {
    require(isEmpty) {
        if (isDirectory) "$this must be empty but has ${list().count()} entries."
        else "$this must be empty but has $size."
    }
}
