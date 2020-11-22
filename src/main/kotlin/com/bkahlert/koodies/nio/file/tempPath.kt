package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.file.Paths.fileNameFrom
import com.imgcstmzr.util.Paths
import java.nio.file.Path

/**
 * Creates a path to a file or directory that [notExists] yet in the system's temp directory.
 *
 * Or in other words: The returned path can be safely used to write data to as nothing is there yet.
 *
 * @see tempDir
 * @see tempFile
 */
fun tempPath(base: String = "", extension: String = ""): Path =
    Paths.TEMP.tempPath(base, extension).requireTempContained()

/**
 * Creates a path to a file or directory that [notExists] yet in this directory.
 *
 * Or in other words: The returned path can be safely used to write data to as nothing is there yet.
 *
 * @see tempDir
 * @see tempFile
 */
tailrec fun Path.tempPath(base: String = "", extension: String = ""): Path {
    val randomPath = resolve(fileNameFrom(base, extension)).requireTempContained()
    if (!randomPath.exists) return randomPath
    return tempPath(base, extension)
}
