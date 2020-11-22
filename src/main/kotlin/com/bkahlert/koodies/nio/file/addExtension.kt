package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.file.Paths.sanitizedExtensionString
import java.nio.file.Path

/**
 * Adds [extension] to this [Path].
 *
 * Example: `Path.of("/path/file.foo").addExtension("bar")` returns path `/path/file.foo.bar`.
 */
fun Path.addExtension(vararg extensions: String): Path {
    Paths.requireNonEmptyExtensions(extensions)
    return resolveSibling(fileName.serialized + sanitizedExtensionString(extensions))
}
