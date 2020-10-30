package com.imgcstmzr.util

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.random
import java.awt.datatransfer.MimeTypeParseException
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.Base64
import kotlin.streams.toList

val Path.exists: Boolean
    get() = if (this is ClassPath) {
        this.exists
    } else {
        toFile().exists()
    }

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
    if (!exists) Files.write(this, emptyList())
    Files.setLastModifiedTime(this, FileTime.from(Instant.now()))
    return this
}

fun Path.mkdirs(): Path {
    toFile().mkdirs()
    return this
}

fun Path.mkRandomDir(): Path = resolve(String.random(4)).mkdirs()

private val Path.fileNameParts: Pair<String, String?>
    get() = fileName.toString().split(".").let {
        if (it.size > 1) it.dropLast(1).joinToString(".") to it.last()
        else it.joinToString(".") to null
    }

fun Path.resourceAsStream(): InputStream? =
    if (this is ClassPath) {
        this.resourceAsStream()
    } else {
        Files.newInputStream(this)
    }

fun Path.resourceAsBufferedStream(): BufferedInputStream? =
    resourceAsStream()?.let { BufferedInputStream(it) }

fun Path.resourceAsBufferedReader(): InputStreamReader? =
    resourceAsBufferedStream()?.let { InputStreamReader(it) }

/**
 * Converts this path using Base64.
 */
fun Path.toBase64(): String = Base64.getEncoder().encodeToString(readAllBytes())

/**
 * Converts this path to a [data URI](https://en.wikipedia.org/wiki/Data_URI_scheme) of the form
 * `data:[<media type>][;base64],<data>`, e.g. `data:image/gif;base64,...`
 *
 * @throws MimeTypeParseException if the mime type cannot be guessed by the resource name
 */
fun Path.toDataUri(): String {
    val mimeType = URLConnection.guessContentTypeFromName(fileName.toString())
    val data = Base64.getEncoder().encodeToString(readAllBytes())
    return "data:$mimeType;base64,$data"
}


/**
 * Sets the content of this file as [text] encoded using UTF-8.
 * If this file exists, it becomes overwritten.
 *
 * @param text text to write into file.
 */
fun Path.writeText(text: String): Unit = toFile().writeText(text)

/**
 * Appends [text] to the content of this file using UTF-8.
 *
 * @param text text to append to file.
 */
fun Path.appendText(text: String): Unit = toFile().appendText(text)

/**
 * Appends a new [line] to the content of this file using UTF-8.
 *
 * @param line line to append to file.
 */
fun Path.appendLine(line: String): Unit = appendText("$line${LineSeparators.LF}")

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
 * Adds [extension] to this [Path].
 *
 * Example: `Path.of("/path/file.foo").addExtension("bar")` returns path `/path/file.foo.bar`.
 */
fun Path.addExtension(extension: String): Path = "$fileName.$extension".let { parent?.resolve(it) ?: Path.of(it) }

/**
 * Removes [extension] from this [Path].
 *
 * Example: `Path.of("/path/file.foo.bar").removeExtension("bar")` returns path `/path/file.foo`.
 *
 * @throws IllegalArgumentException if the [extension] to be removed is not present
 */
fun Path.removeExtension(extension: String): Path {
    require("$fileName".endsWith(".$extension")) { "$this must end with .$extension" }
    return "$fileName".dropLast(extension.length + 1).let { parent?.resolve(it) ?: Path.of(it) }
}

/**
 * Returns the name of the file described by this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.fileNameWithExtension(extension: String): String = "$baseName.$extension"

/**
 * Returns this [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.withExtension(extension: String): Path = parent.resolve(fileNameWithExtension(extension))

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

fun Path.readAllBytes(): ByteArray = if (this is ClassPath) this.readAllBytes() else Files.readAllBytes(this)

fun Path.readAllLines(charset: Charset = Charsets.UTF_8): List<String> =
    if (this is ClassPath) this.readAllLines(charset) else Files.readAllLines(this, charset)

fun Path.readAll(): String = String(readAllBytes())

fun Path.copyTo(dest: Path, createDirectories: Boolean = true): Path =
    if (this is ClassPath) {
        this.copyTo(dest, createDirectories)
    } else {
        if (createDirectories) Files.createDirectories(dest.parent)
        Files.copy(this, dest)
    }

fun Path.moveTo(dest: Path, createDirectories: Boolean = true): Path =
    if (this is ClassPath) {
        throw IllegalArgumentException("$this is a ${ClassPath::class.simpleName} which is read-only and therefore cannot be moved (but copied).")
    } else {
        if (createDirectories) Files.createDirectories(dest.parent)
        Files.move(this, dest)
    }

fun Path.renameTo(fileName: String): Path =
    if (this is ClassPath) {
        throw IllegalArgumentException("$this is a ${ClassPath::class.simpleName} which is read-only and therefore cannot be renamed.")
    } else {
        requireExists()
        toFile().let {
            val destination = File(it.parent, fileName)
            it.renameTo(destination)
            destination.toPath()
        }
    }

/**
 * Cleans up this path by removing the [delimiter] from the [Path.getFileName].
 *
 * The removal itself takes place by renaming the corresponding file system resource.
 */
fun Path.cleanUp(delimiter: String): Path = "$fileName".let {
    if (it.contains(delimiter)) renameTo(it.substringBefore(delimiter))
    else this
}

fun Path.copyToDirectory(dest: Path, createDirectories: Boolean = true): Path {
    require(dest.isDirectory || !dest.exists) { "$dest must be a directory" }
    return copyTo(dest.resolve(fileName), createDirectories)
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
    val tempFile = File.createTempFile(fileNameParts.first + "-", fileNameParts.second?.let { ".$it" }).also { it.deleteOnExit() }.toPath()
    return if (isolated) tempFile.parent.mkRandomDir().let { Files.move(tempFile, it.resolve(tempFile.fileName)) }
    else tempFile
}

/**
 * Returns a temporary copy of this [Path] optionally residing in an [isolated], that is, otherwise empty directory.
 */
fun Path.copyToTempFile(isolated: Boolean = false): Path {
    val tempFile = asEmptyTempFile(isolated)
    tempFile.delete()
    copyTo(tempFile, true)
    return tempFile
}

/**
 * Returns a temporary copy of this paths in a sibling directory with random name and a modified file name.
 */
fun Path.copyToTempSiblingDirectory(): Path {
    val random = String.random(4)
    val siblingDir: Path = parent.resolveSibling("${parent.fileName}-$random").mkdirs()
    val siblingFile: Path = siblingDir.resolve("${fileNameParts.first}-$random" + if (fileNameParts.second != null) ".${fileNameParts.second}" else "")
    copyTo(siblingFile, true)
    return siblingFile
}

/**
 * Returns a list of all directories and files in this [Path] **and its sub directories** that satisfy the provided [predicate].
 */
fun Path.listFilesRecursively(predicate: ((path: Path) -> Boolean) = { true }, comparator: Comparator<Path> = naturalOrder()): List<Path> =
    Files.walk(this).use { it.sorted(comparator).filter(predicate).toList() }

/**
 * Calls [action] on each directory and file in this [Path] **and its sub directories**.
 */
fun Path.onEachFileRecursively(action: (path: Path) -> Unit, comparator: Comparator<Path> = naturalOrder()) {
    Files.walk(this).use { it.sorted(comparator).forEach(action) }
}

/**
 * Calls [action] on each directory/file in this [Path] (including its sub directories)
 * and returns a [Sequence] of each [action]'s result.
 */
fun <R> Path.mapFilesRecursively(action: (path: Path) -> R, comparator: Comparator<Path> = naturalOrder()): List<R> =
    Files.walk(this).use { it.sorted(comparator).map(action).toList() }

fun Path.delete(recursively: Boolean = false) {
    if (recursively) {
        if (exists) toFile().deleteRecursively()
    } else {
        if (exists) toFile().delete()
    }
}

/**
 * Requires this path to be a directory and contain exactly one file which
 * will be returned.
 *
 * @throws IllegalArgumentException with [lazyMessage] if not a single file was found
 */
fun Path.checkSingleFile(lazyMessage: () -> Any): Path = toFile().listFiles()?.let {
    check(it.size == 1, lazyMessage)
    it.first().toPath()
} ?: throw IllegalStateException("${lazyMessage()}")
