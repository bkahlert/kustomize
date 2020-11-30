package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.random
import java.nio.file.Path

/**
 * Duplicates this file or directory by copying to the same path but with a random string to its name.
 *
 * In contrast to [copyTo] this method allows to specify the [order], that is, by how many ancestors should
 * the path segments differ from this path.
 *
 * - A order of `0` is identical to a making a copy with [copyTo].
 * - `1` (*default*) appends the suffix to parent's [Path.getFileName] instead.
 * - `2` to parent's parent
 * - ...  and so on
 *
 * E.g. `/a/b/c`'s 2 order duplication can be found at `/a/b-random/c`.
 */
fun Path.duplicate(order: Int = 1, suffix: String = "-${String.random(4)}"): Path {
    val sibling = resolveSibling(order) { resolveSibling(fileName.serialized + suffix) }
    return copyTo(sibling)
}
