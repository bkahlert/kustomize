package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.string.withoutPrefix
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths as NioPaths

/**
 * Functions to access unknown and well known paths.
 */
object Paths {

    internal fun fileNameFrom(base: String, extension: String): String {
        val minLength = 6
        val length = base.length + extension.length
        val randomLength = (minLength - length).coerceAtLeast(3)
        return "$base${String.random(randomLength)}$extension"
    }

    internal fun requireNonEmptyExtensions(extensions: Array<out String>) {
        require(extensions.isNotEmpty()) { "At least one extension must be provided." }
    }

    internal fun sanitizeExtensions(extensions: Array<out String>) =
        extensions.flatMap { it.withoutPrefix(".").split(".") }

    internal fun sanitizedExtensionString(extensions: Array<out String>) =
        sanitizeExtensions(extensions).joinToString(prefix = ".", separator = ".")

    /**
     * Attempts to parse [path] as an [URI] and convert it to a [Path].
     *
     * If parsing fails, converts the [path] string (and if specified joining it with [more]) to a [Path].
     *
     * @see Paths.get
     * @see Path.of
     */
    operator fun get(path: String, vararg more: String): Path =
        kotlin.runCatching { get(URI.create(path)) }
            .recover {
                if (path.startsWith("classpath:")) {
                    val delegate by classPath(path)
                    delegate
                } else NioPaths.get(path, *more)
            }.getOrThrow()

    /**
     * Converts the given URI to a [Path].
     *
     * @see Paths.get
     * @see Path.of
     */
    operator fun get(uri: URI): Path = NioPaths.get(uri)

    /**
     * Working directory, that is, the directory in which this binary can be found.
     */
    val WorkingDirectory: Path = FileSystems.getDefault().getPath("").toAbsolutePath()

    /**
     * Home directory of the currently logged in user.
     */
    val HomeDirectory: Path = Path.of(System.getProperty("user.home"))

    /**
     * Directory in which temporary data can be stored.
     */
    val Temp: Path = Path.of(System.getProperty("java.io.tmpdir"))
}
