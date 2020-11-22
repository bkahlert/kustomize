package com.imgcstmzr.util

import com.bkahlert.koodies.nio.file.copyTo
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.lastModified
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.readBytes
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.time.Now
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList
import com.bkahlert.koodies.nio.file.delete as del

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

fun Path.mkRandomDir(): Path = resolve(String.random(4)).mkdirs()

private val Path.fileNameParts: Pair<String, String?>
    get() = fileName.serialized.split(".").let {
        if (it.size > 1) it.dropLast(1).joinToString(".") to it.last()
        else it.joinToString(".") to null
    }

/**
 * Returns the base name of the file described by this [Path].
 * Example: `/path/file.pdf` would return `file`.
 */
val Path.baseName: String get() = fileNameParts.first

/**
 * Returns the extension of the file described by this [Path].
 * Example: `/path/file.pdf` would return `pdf`.
 *
 * If no extension is present, `null` is returned.
 */
val Path.extension: String? get() = fileNameParts.second


/**
 * Returns the name of the file described by this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.fileNameWithExtension(extension: String): String = "$baseName.$extension"

/**
 * Returns this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.withExtension(extension: String): Path {
    val resolveSibling = resolveSibling(fileNameWithExtension(extension))
    return resolveSibling
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

val Path.quoted get() = toAbsolutePath().toString().quoted

@Deprecated("", replaceWith = ReplaceWith("readBytes()", "com.bkahlert.koodies.nio.file.readBytes"))
fun Path.readAllBytes(): ByteArray = readBytes()

fun Path.readAllLines(charset: Charset = Charsets.UTF_8): List<String> =
    Files.readAllLines(this, charset)

@Deprecated("", replaceWith = ReplaceWith("readText()", "com.bkahlert.koodies.nio.file.readText"))
fun Path.readAll(): String = String(readAllBytes())

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
 * Returns a temporary empty file of which the filename is derived from this path.
 *
 * Furthermore if [isolated] is set to `true`, the returned path is guaranteed only include this single file.
 *
 * Example: `/path/file.ext` becomes `/var/folders/hh/2o87sdl12piu3jeo/T/dldk12aj3sk4/file-78363289732697283.ext`
 */
fun Path.asEmptyTempFile(isolated: Boolean = false): Path {
    val fileNameParts = fileNameParts
    val tempFile = com.bkahlert.koodies.nio.file.tempFile(fileNameParts.first + "-", fileNameParts.second?.let { ".$it" } ?: "")
        .apply { toFile().deleteOnExit() }
    return if (isolated) tempFile.parent.mkRandomDir().let { Files.move(tempFile, it.resolve(tempFile.fileName)) }
    else tempFile
}

/**
 * Returns a temporary copy of this [Path] optionally residing in an [isolated], that is, otherwise empty directory.
 */
fun Path.copyToTempFile(isolated: Boolean = false): Path {
    val tempFile = asEmptyTempFile(isolated)
    tempFile.del()
    return copyTo(tempFile)
}

/**
 * Returns a temporary copy of this paths in a sibling directory with random name and a modified file name.
 */
fun Path.copyToTempSiblingDirectory(): Path {
    val random = String.random(4)
    val siblingDir: Path = resolveSibling("${parent.fileName}-$random").mkdirs()
    val siblingFile: Path = siblingDir.resolve("${fileNameParts.first}-$random" + if (fileNameParts.second != null) ".${fileNameParts.second}" else "")
    return copyTo(siblingFile)
}

/**
 * Returns a list of all directories and files in this [Path] **and its sub directories** that satisfy the provided [predicate].
 */
fun Path.listFilesRecursively(predicate: ((path: Path) -> Boolean) = { true }, comparator: Comparator<Path> = naturalOrder()): List<Path> =
    Files.walk(toAbsolutePath()).use { it.sorted(comparator).filter(predicate).toList() }
