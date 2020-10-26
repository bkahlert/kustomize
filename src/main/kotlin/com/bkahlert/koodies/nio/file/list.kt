package com.bkahlert.koodies.nio.file

import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/**
 * Return a lazily populated [Sequence], the elements of which are the entries in the directory.
 *
 * The listing is not recursive.
 *
 * @see [Files.list]
 */
fun Path.list(): Sequence<Path> = Files.list(this).asSequence()
