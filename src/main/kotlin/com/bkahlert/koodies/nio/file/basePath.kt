package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Returns the base path of the file described by this [Path].
 * Example: `/path/file.pdf` would return `/path/file`.
 *
 * @see [baseName]
 */
val Path.basePath: Path
    get() {
        return fileName.extensionIndex.let { extensionIndex ->
            if (extensionIndex >= 0) resolveSibling(fileName.serialized.take(extensionIndex))
            else this
        }
    }
