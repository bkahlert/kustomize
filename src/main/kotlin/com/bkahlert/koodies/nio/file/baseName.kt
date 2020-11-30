package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Returns the base name of the file described by this [Path].
 * Example: `/path/file.pdf` would return `file`.
 *
 * @see [basePath]
 */
val Path.baseName: Path
    get() = fileName.basePath
