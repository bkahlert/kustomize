package com.imgcstmzr.util

import java.nio.file.Path

object Paths {
    val USER_HOME: Path = Path.of(System.getProperty("user.home"))
    val TEMP: Path = Path.of(System.getProperty("java.io.tmpdir"))

    val CACHE: Path by lazy { USER_HOME.resolve(".imgcstmzr") }
    val TEST: Path by lazy { USER_HOME.resolve(".imgcstmzr.test") }

    fun of(paths: List<Path>): Path = Path.of(paths.get(0).toString(), *paths.drop(1).map { toString() }.toTypedArray())
}
