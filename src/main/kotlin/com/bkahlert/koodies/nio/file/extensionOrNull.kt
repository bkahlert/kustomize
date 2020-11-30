package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Returns the extension of the file described by this [Path].
 * Example: `/path/file.pdf` would return `pdf`.
 *
 * If no extension is present, `null` is returned.
 */
val Path.extensionOrNull: String?
    get() = extensionIndex.takeIf { it >= 0 }?.let { serialized.drop(it + 1) }
