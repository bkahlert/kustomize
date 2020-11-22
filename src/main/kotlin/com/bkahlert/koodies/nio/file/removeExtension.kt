package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.extension
import java.nio.file.Path

/**
 * Removes [extension] from this [Path].
 *
 * Example: `Path.of("/path/file.foo.bar").removeExtension("bar")` returns path `/path/file.foo`.
 *
 * @throws IllegalArgumentException if the [extension] to be removed is not present
 */
fun Path.removeExtension(vararg extensions: String): Path {
    Paths.requireNonEmptyExtensions(extensions)
    require(hasExtension(*extensions)) { "$this must have extension ${extensions.joinToString(", ")}" }
    return resolveSibling(fileName.serialized.dropLast(Paths.sanitizedExtensionString(extensions).count()))
}
