package com.bkahlert.koodies.nio.exception

import com.bkahlert.koodies.nio.file.serialized
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * Constructs an instance of [NoSuchFileException]
 * with an unknown path and an optional [reason].
 */
fun noSuchFile(reason: String? = null): NoSuchFileException =
    NoSuchFileException(null, null, reason)

/**
 * Constructs an instance of [NoSuchFileException]
 * with [path] as the missing path and an optional [reason].
 */
fun noSuchFile(path: Path, reason: String? = null): NoSuchFileException =
    NoSuchFileException(path.serialized, null, reason)

/**
 * Constructs an instance of [NoSuchFileException]
 * with [source] and [target] as the missing paths and an optional [reason].
 */
fun noSuchFile(source: Path, target: Path, reason: String? = null): NoSuchFileException =
    NoSuchFileException(source.serialized, target.serialized, reason)
