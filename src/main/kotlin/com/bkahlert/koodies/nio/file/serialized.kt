package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * String representation of this path that does **not** rely on [toString].
 */
val Path.serialized: String get() = "${resolve("")}"
