package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.exception.noSuchFile
import java.nio.file.Path

/**
 * Throws if this [Path] does not exist.
 */
fun Path.requireExists() {
    if (notExists) throw noSuchFile()
}
