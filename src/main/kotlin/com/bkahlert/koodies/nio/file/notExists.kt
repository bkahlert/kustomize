package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Whether this path not exists.
 *
 * *Hint:* This implementation—as it should be—does not rely on [toString] so
 * you're free to override [toString] and/or extend [Path].
 */
val Path.notExists: Boolean get() = !exists
