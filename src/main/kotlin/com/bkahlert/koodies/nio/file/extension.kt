package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Returns the extension of the file described by this [Path] with a leading `.`.
 * Example: `/path/file.pdf` would return `.pdf`.
 *
 * If no extension is present, an empty string is returned.
 */
val Path.extension: String
    get() = extensionOrNull?.let { ".$it" } ?: ""
