package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Returns this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.withExtension(extension: String): Path =
    resolveSibling(fileNameWithExtension(extension))
