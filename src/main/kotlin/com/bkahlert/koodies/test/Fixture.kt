package com.bkahlert.koodies.test

import com.bkahlert.koodies.nio.file.classPath
import com.bkahlert.koodies.nio.file.copyTo
import com.bkahlert.koodies.nio.file.copyToDirectory
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.tempPath
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.baseName
import com.imgcstmzr.util.extension
import com.imgcstmzr.util.quoted
import java.nio.file.Path

open class Fixture(val path: String) {

    val fileName: Path by lazy { Path.of(path).fileName }

    val text: String by lazy {
        classPath(path, fun Path.(): String = readText())
            ?: error("Error reading ${path.quoted}")
    }

    fun copyToTemp(
        base: String = "${fileName.baseName}.",
        extension: String = fileName.extension?.let { ".$it" } ?: "",
    ): Path = copyTo(Paths.TEMP.tempPath(base, extension))

    fun copyTo(target: Path): Path = classPath(path, fun Path.(): Path = copyTo(target))
        ?: error("Error copying ${path.quoted} to ${target.quoted}")

    fun copyToDirectory(target: Path): Path = classPath(path, fun Path.(): Path = copyToDirectory(target))
        ?: error("Error copying ${path.quoted} to directory ${target.quoted}")

    inline operator fun <reified T> invoke(crossinline transform: Path.() -> T) = classPath(path, transform)
        ?: error("Error processing ${path.quoted}")

    open inner class SubFixture(subPath: String) : Fixture("$path/$subPath")
}
