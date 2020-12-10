package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Copies a file or directory located by this path to the given target path.
 *
 * In contract to [copyTo] this method tries a [clone copy](https://www.unix.com/man-page/mojave/2/clonefile/) first.
 */
fun Path.cloneTo(target: Path): Path {
    return if (Paths.clonefileSupport) {
        if (target.exists) throw fileAlreadyExists(this, target)
        Runtime.getRuntime()?.exec(arrayOf("cp", "-c", serialized, target.serialized))?.waitFor()?.let { exitValue ->
            check(exitValue == 0) { "Cloning failed with $exitValue" }
            target
        } ?: throw IllegalStateException("Error executing file cloning")
    } else {
        copyTo(target)
    }
}

