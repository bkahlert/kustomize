package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.random
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.touch
import java.nio.file.Path

fun tempFile(base: String = String.random(), extension: String = ".txt"): Path =
    createTempFile(base, extension).toPath()

fun Path.tempFile(base: String = String.random(), extension: String = ".txt"): Path =
    (takeIf { exists } ?: mkdirs()).resolve("$base${String.random(8)}$extension").touch()
