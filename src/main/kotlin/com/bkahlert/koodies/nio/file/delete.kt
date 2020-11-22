package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.isDirectory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Deletes this path. If [recursively] is provided and this path is a directory,
 * this directory and all of its content is deleted.
 *
 * Returns the deletes path.
 */
fun Path.delete(recursively: Boolean = false): Path =
    apply {
        if (exists) {
            if (recursively && isDirectory) walkBottomUp().forEach { it.delete(false) }
            else Files.delete(this)
        }
    }
