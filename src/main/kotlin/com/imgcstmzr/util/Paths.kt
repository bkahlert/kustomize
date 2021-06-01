package com.imgcstmzr.util

import com.imgcstmzr.Locations
import java.nio.file.Path

object Paths {
    val CACHE: Path by lazy { Locations.HomeDirectory.resolve(".imgcstmzr") }
    val TEST: Path by lazy { Locations.HomeDirectory.resolve(".imgcstmzr.test") }
}
