package com.bkahlert.koodies.nio.exception

import com.bkahlert.koodies.nio.file.serialized
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Path


/**
 * Constructs an instance of [DirectoryNotEmptyException]
 * with this path.
 */
@JvmName("receiverDirectoryNotEmpty")
fun Path.directoryNotEmpty(): DirectoryNotEmptyException =
    DirectoryNotEmptyException(serialized)

/**
 * Constructs an instance of [DirectoryNotEmptyException]
 * with [path] as the not empty directory.
 */
fun directoryNotEmpty(path: Path): DirectoryNotEmptyException =
    DirectoryNotEmptyException(path.serialized)
