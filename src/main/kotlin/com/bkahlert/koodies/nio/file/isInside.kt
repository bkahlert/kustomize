package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Returns whether this path is inside [path].
 */
fun Path.isInside(path: Path): Boolean =
    normalize().toAbsolutePath().startsWith(path.normalize().toAbsolutePath())
