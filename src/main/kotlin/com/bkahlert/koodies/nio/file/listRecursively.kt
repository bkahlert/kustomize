package com.bkahlert.koodies.nio.file

import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/**
 * Returns a lazily populated [Sequence], the elements of which are the entries in this directory and recursively their sub directories.
 *
 * The listing is recursive. Use [list] to limit the enumeration to immediate children.
 *
 * @see [list]
 * @see [Files.walk]
 */
fun Path.listRecursively(): Sequence<Path> {
    requireDirectory()
    return Files.walk(this).asSequence().filter { it != this }
}
