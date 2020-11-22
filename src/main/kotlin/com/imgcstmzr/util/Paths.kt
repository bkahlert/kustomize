package com.imgcstmzr.util

import java.nio.file.Path
import com.bkahlert.koodies.nio.file.Paths as KoodiesPaths

object Paths {

    val WORKING_DIRECTORY: Path = KoodiesPaths.WorkingDirectory
    val USER_HOME: Path = KoodiesPaths.HomeDirectory
    val TEMP: Path = KoodiesPaths.Temp

    val CACHE: Path by lazy { USER_HOME.resolve(".imgcstmzr") }
    val TEST: Path by lazy { USER_HOME.resolve(".imgcstmzr.test") }

    fun of(paths: List<Path>): Path = Path.of(paths[0].toString(), *paths.drop(1).map { toString() }.toTypedArray())

}
