package com.imgcstmzr.util

import java.nio.file.Path
import com.bkahlert.koodies.nio.file.Paths as KoodiesPaths

object Paths {

    val CACHE: Path by lazy { KoodiesPaths.HomeDirectory.resolve(".imgcstmzr") }
    val TEST: Path by lazy { KoodiesPaths.HomeDirectory.resolve(".imgcstmzr.test") }

}
