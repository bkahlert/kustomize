package com.bkahlert.koodies.nio.file

import java.net.URI
import java.net.URL
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

private val lock = Any()

private fun <T> URI.internalToMappedPath(transform: (Path) -> T): T =
    runCatching {
        transform(Paths.get(this))
    }.recoverCatching { ex ->
        if (ex !is FileSystemNotFoundException) throw ex
        FileSystems.newFileSystem(this, emptyMap<String, Any>()).use { fs ->
            transform(fs.provider().getPath(this))
        }
    }.getOrThrow()

fun <T> URI.synchronizedToMappedPath(transform: (Path) -> T): T =
    synchronized(lock) { internalToMappedPath(transform) }

/**
 * Gets the [Path] this [URI] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] and [Path.of] this function does not
 * only check the default file system but also loads to needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
inline fun <reified T> URI.toMappedPath(noinline transform: (Path) -> T): T =
    synchronizedToMappedPath(transform)

/**
 * Gets the [Path] this [URL] points to and applies [transform] to it.
 *
 * In contrast to [Paths.get] and [Path.of] this function does not
 * only check the default file system but also loads to needed one if necessary
 * (and closes it afterwards).
 *
 * @see FileSystems.getDefault
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
inline fun <reified T> URL.toMappedPath(noinline transform: (Path) -> T): T =
    toURI().toMappedPath(transform)