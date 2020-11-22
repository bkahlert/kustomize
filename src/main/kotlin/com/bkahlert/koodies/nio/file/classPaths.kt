package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.contextClassLoader
import com.bkahlert.koodies.string.withoutPrefix
import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Gets the class path resources, the specified [path] points to and applies [transform] to each.
 *
 * Also this function does its best to avoid write access by wrapping the
 * actual [FileSystem] with a write protection layer. **Write protection
 * also covers paths generated from the one provided during the [transform] call.
 *
 * @see <a href="https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource"
 * >java.nio.file.Path for a classpath resource</a>
 */
inline fun <reified T> classPaths(path: String, crossinline transform: Path.() -> T): List<T> {
    val normalizedPath = path.withoutPrefix("classpath:", ignoreCase = true).withoutPrefix("/")
    return contextClassLoader.getResources(normalizedPath).asSequence().map { url ->
        url.toMappedPath { classPath -> classPath.asReadOnly().transform() }
    }.toList()
}
