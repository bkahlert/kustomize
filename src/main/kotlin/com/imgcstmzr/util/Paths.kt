package com.imgcstmzr.util

import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.unit.size
import java.nio.file.Path

object Paths {
    fun tempFile(base: String = String.random(), extension: String = ".txt"): Path = createTempFile(base, extension).toPath()

    val WORKING_DIRECTORY: Path = Path.of("").toAbsolutePath()
    val USER_HOME: Path = Path.of(System.getProperty("user.home"))
    val TEMP: Path = Path.of(System.getProperty("java.io.tmpdir"))

    val CACHE: Path by lazy { USER_HOME.resolve(".imgcstmzr") }
    val TEST: Path by lazy { USER_HOME.resolve(".imgcstmzr.test") }

    fun of(paths: List<Path>): Path = Path.of(paths[0].toString(), *paths.drop(1).map { toString() }.toTypedArray())

    val fileSizeComparator: (Path, Path) -> Int = { path1, path2 -> path1.size.compareTo(path2.size) }
}
