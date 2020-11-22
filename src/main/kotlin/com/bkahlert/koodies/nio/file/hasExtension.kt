package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.file.Paths.requireNonEmptyExtensions
import com.bkahlert.koodies.nio.file.Paths.sanitizeExtensions
import java.nio.file.Path

/**
 * Returns whether this path [Path.getFileName] is the specified [extensions].
 *
 * The extension is compared ignoring the case and an eventually leading period by default.
 *
 * That is, `.ext`, `ext`, `.EXT` and `EXT` are all treated the same way.
 */
fun Path.hasExtension(vararg extensions: String, ignoreCase: Boolean = true): Boolean {
    requireNonEmptyExtensions(extensions)
    val actualExtensions = fileName.serialized.split(".")
    val sanitizedExtensions = sanitizeExtensions(extensions)
    if (actualExtensions.size <= sanitizedExtensions.size) return false
    return actualExtensions.takeLast(sanitizedExtensions.size).zip(sanitizedExtensions).fold(true) { identical, (actualExtension, extension) ->
        identical && actualExtension.equals(extension, ignoreCase = ignoreCase)
    }
}
