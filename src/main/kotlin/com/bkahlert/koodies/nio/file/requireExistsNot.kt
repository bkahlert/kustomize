package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Throws if this [Path] does exist.
 */
fun Path.requireExistsNot() {
    require(notExists) { "$this must not exist but does." }
}
