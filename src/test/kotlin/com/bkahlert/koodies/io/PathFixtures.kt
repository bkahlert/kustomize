package com.bkahlert.koodies.io

import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.util.Paths

object PathFixtures {
    fun directoryWithTwoFiles() = Paths.tempDir()
        .also { ClassPath("example.html").copyTo(it.resolve("example.html")) }
        .also { ClassPath("config.txt").copyTo(it.resolve("sub-dir/config.txt")) }
}
