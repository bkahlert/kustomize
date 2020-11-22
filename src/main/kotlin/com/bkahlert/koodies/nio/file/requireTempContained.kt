package com.bkahlert.koodies.nio.file

import java.nio.file.Path

fun Path.requireTempContained(): Path =
    apply { require(!isDefaultFileSystem || isInside(Paths.Temp)) { "${this.normalize().toAbsolutePath()} is not inside ${Paths.Temp}." } }
