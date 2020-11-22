package com.bkahlert.koodies.nio.file

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * Contains whether this path belongs to the default [FileSystem].
 *
 * @see [FileSystems.getDefault]
 */
val Path.isDefaultFileSystem: Boolean get() = fileSystem == FileSystems.getDefault()
