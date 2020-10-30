package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import java.nio.file.Files
import java.nio.file.Path

/**
 * Whether this path exists.
 *
 * *Hint:* This implementation—as it should be—does not rely on [toString] so
 * you're free to override [toString] and/or extend [Path].
 */
val Path.exists: Boolean
    get() =
        if (this is ClassPath) this.exists
        else Files.exists(toAbsolutePath())
