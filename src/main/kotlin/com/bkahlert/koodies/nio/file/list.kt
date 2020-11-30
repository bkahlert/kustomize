package com.bkahlert.koodies.nio.file

import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/**
 * Return a lazily populated [Sequence], the elements of which are the entries in the directory.
 *
 * The listing is not recursive. Use [listRecursively] to list the whole sub tree.
 *
 * @see [listRecursively]
 * @see [Files.list]
 */
fun Path.list(): Sequence<Path> = Files.list(this).asSequence()

/**
 * Return an [Array], the elements of which are the entries in the directory.
 *
 * The listing is not recursive. Use [listRecursively] to list the whole sub tree.
 *
 * @see [listRecursively]
 * @see [Files.list]
 */
fun Path.listAsArray(): Array<Path> = Files.list(this).toArray { size -> arrayOfNulls<Path>(size) }
