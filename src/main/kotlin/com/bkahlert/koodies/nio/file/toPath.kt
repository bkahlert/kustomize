package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Converts this char sequence to a [Path].
 */
fun CharSequence.toPath(): Path = Paths[this.toString()]
