package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.random
import com.imgcstmzr.util.mkdirs
import java.nio.file.Path

fun tempDir(base: String = String.random(), extension: String = "-tmp"): Path =
    createTempDir(base, extension).toPath()

fun Path.tempDir(base: String = String.random(), extension: String = "-tmp"): Path =
    (takeIf { exists } ?: mkdirs()).resolve("$base${String.random(8)}$extension")?.mkdirs() ?: createTempDir(base, extension).toPath()
