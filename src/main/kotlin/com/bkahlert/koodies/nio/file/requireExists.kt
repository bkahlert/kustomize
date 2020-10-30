package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Throws if this [Path] does not exist.
 */
fun Path.requireExists() {
    require(exists) { "$this must exist but does not." }
}
