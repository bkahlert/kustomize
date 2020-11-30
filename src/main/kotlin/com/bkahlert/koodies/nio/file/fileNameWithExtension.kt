package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Returns the name of the file described by this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.fileNameWithExtension(extension: String): String = "$baseName.$extension"
