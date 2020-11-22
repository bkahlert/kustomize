package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.file.Paths.fileNameFrom
import java.nio.file.Path

/**
 * Creates an empty directory in the system's temp directory.
 *
 * @see tempFile
 */
fun tempDir(base: String = "", extension: String = "-tmp"): Path =
    createTempDir(fileNameFrom(base, ""), extension).toPath().requireTempContained()

/**
 * Creates an empty directory in this directory.
 *
 * @see tempFile
 */
fun Path.tempDir(base: String = "", extension: String = "-tmp"): Path =
    resolve(fileNameFrom(base, extension)).requireTempContained()
        .mkdirs()
