package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.isFile
import java.nio.file.Path

/**
 * Requires this to be a file and throws an [IllegalArgumentException] otherwise.
 */
fun Path.requireFile() {
    require(isFile) { "$this is no file." }
}
