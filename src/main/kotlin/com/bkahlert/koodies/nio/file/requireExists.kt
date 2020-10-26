package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.exists
import java.nio.file.Path

/**
 * Throws if this [Path] does not exist.
 */
fun Path.requireExists() {
    require(exists) { "$this must exist but does not." }
}
