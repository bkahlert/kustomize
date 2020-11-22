package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.file.Paths.fileNameFrom
import com.imgcstmzr.util.touch
import java.nio.file.Path

/**
 * Creates an empty file in the system's temp directory.
 *
 * @see tempDir
 */
fun tempFile(base: String = "", extension: String = ".tmp"): Path =
    createTempFile(fileNameFrom(base, ""), extension).toPath().requireTempContained()

/**
 * Creates an empty file in this directory.
 *
 * @see tempDir
 */
fun Path.tempFile(base: String = "", extension: String = ".tmp"): Path =
    resolve(fileNameFrom(base, extension)).requireTempContained()
        .apply { parent.mkdirs() }
        .apply { touch() }
