package com.imgcstmzr.util

import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.lastModified
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.string.wrap
import com.bkahlert.koodies.time.Now
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

val Path.isReadable: Boolean
    get() = Files.isReadable(this)

val Path.isWritable: Boolean
    get() = Files.isWritable(this)

val Path.isExecutable: Boolean
    get() = Files.isExecutable(this)

fun Path.makeExecutable() = toFile().setExecutable(true)

val Path.isFile: Boolean
    get() = Files.isRegularFile(this)

val Path.isDirectory: Boolean
    get() = Files.isDirectory(this)

val Path.isSymlink: Boolean
    get() = Files.isSymbolicLink(this)

val Path.isHidden: Boolean
    get() = Files.isHidden(this)

fun Path.touch(): Path {
    if (!exists) writeText("")
    lastModified = Now.fileTime
    return this
}

/**
 * Constructs a path that takes this path as the root of [subPath].
 *
 * **Example 1: "re-rooting an absolute path"** ➜
 * `"/dir/a".asRootFor("/dir/b")` becomes `"/dir/a/dir/b"`
 *
 * **Example 2: "re-rooting a relative path"** ➜
 * `"/dir/b".asRootFor("dir/b")` becomes `"/dir/a/dir/b"`
 *
 */
fun Path.asRootFor(subPath: Path): Path {
    val nameCount = subPath.nameCount
    return if (nameCount > 0) resolve(subPath.subpath(0, nameCount)) else this
}

fun Path.wrap(value: CharSequence): String = toString().wrap(value)

fun Path.moveTo(dest: Path, createDirectories: Boolean = true): Path =
    run {
        if (createDirectories) Files.createDirectories(dest.parent)
        Files.move(this, dest)
    }

fun Path.renameTo(fileName: String): Path = run {
    requireExists()
    Files.move(this, this.resolveSibling(fileName))
}

/**
 * Cleans up this path by removing the [delimiters] from the [Path.getFileName]
 * (e.g. `/path/file.txt$crap` with `$` removed ➜ `/path/file.txt`).
 *
 * The removal itself takes place by renaming the corresponding file system resource.
 */
fun Path.cleanUp(vararg delimiters: String = arrayOf("?", "#")): Path {
    val cleanedUp = delimiters.fold(fileName.serialized) { fileName, delimiter -> fileName.substringBefore(delimiter) }
    return renameTo(cleanedUp)
}

/**
 * Returns a list of all directories and files in this [Path] **and its sub directories** that satisfy the provided [predicate].
 */
fun Path.listFilesRecursively(predicate: ((path: Path) -> Boolean) = { true }, comparator: Comparator<Path> = naturalOrder()): List<Path> =
    Files.walk(toAbsolutePath()).use { it.sorted(comparator).filter(predicate).toList() }
