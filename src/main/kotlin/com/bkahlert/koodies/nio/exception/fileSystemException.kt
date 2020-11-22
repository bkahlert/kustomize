package com.bkahlert.koodies.nio.exception

import com.bkahlert.koodies.nio.file.serialized
import java.nio.file.FileSystemException
import java.nio.file.Path

/**
 * Constructs an instance of [FileSystemException]
 * with an unknown path and an optional [reason].
 */
fun fileSystemException(reason: String? = null): FileSystemException =
    FileSystemException(null, null, reason)

/**
 * Constructs an instance of [FileSystemException]
 * with [path] as the affected path and an optional [reason].
 */
fun fileSystemException(path: Path, reason: String? = null): FileSystemException =
    FileSystemException(path.serialized, null, reason)

/**
 * Constructs an instance of [FileSystemException]
 * with [source] and [target] as the affected paths and an optional [reason].
 */
fun fileSystemException(source: Path, target: Path, reason: String? = null): FileSystemException =
    FileSystemException(source.serialized, target.serialized, reason)
