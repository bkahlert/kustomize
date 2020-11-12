package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import java.nio.file.Path

fun CharSequence.toPath(): Path =
    if (startsWith("${ClassPath.SCHEMA}:")) ClassPath.of(this.toString())
    else Path.of(this.toString())
