package com.bkahlert.koodies.nio.file

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path


/**
 * Constructs an instance of [FileAlreadyExistsException]
 * with this path.
 */
fun Path.fileAlreadyExists(): FileAlreadyExistsException =
    FileAlreadyExistsException(serialized)

/**
 * Constructs an instance of [FileAlreadyExistsException]
 * with an unknown path and an optional [reason].
 */
fun fileAlreadyExists(reason: String? = null): FileAlreadyExistsException =
    FileAlreadyExistsException(null, null, reason)

/**
 * Constructs an instance of [FileAlreadyExistsException]
 * with [path] as the already existing file and an optional [reason].
 */
fun fileAlreadyExists(path: Path, reason: String? = null): FileAlreadyExistsException =
    FileAlreadyExistsException(path.serialized, null, reason)

/**
 * Constructs an instance of [FileAlreadyExistsException]
 * with [source] and [target]  path and an optional [reason]
 */
fun fileAlreadyExists(source: Path, target: Path, reason: String? = null): FileAlreadyExistsException =
    FileAlreadyExistsException(source.serialized, target.serialized, reason)
