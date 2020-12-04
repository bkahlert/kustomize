package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.Paths
import java.nio.file.Path

/**
 * Creates a temporary file with the following traits:
 * 1) the file is stored where the OS stores temporary
 * 2) given the same name this function returns the same path
 * 3) the file does not automatically get deleted
 * at the same place with as few assumptions about the OS
 * as possible.
 */
fun sameFile(name: String): Path = Paths.TEMP.resolve(name)