package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.exists
import java.nio.file.Path

/**
 * Throws if this [Path] does exist.
 */
fun Path.requireExistsNot() {
    require(!exists) { "$this must not exist but does." }
}
