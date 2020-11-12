package com.bkahlert.koodies.nio

import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList

class ClassPath(val path: String) : Path by Path.of("$SCHEMA:$path") {
    companion object {
        const val SCHEMA: String = "classpath"
        private const val PREFIX = "$SCHEMA:"
        private val PREFIX_PATTERN = Regex("^$PREFIX+")
        fun of(path: String): ClassPath = ClassPath(path.replaceFirst(PREFIX_PATTERN, ""))
    }

    val exists: Boolean get() = resourceAsStream() != null

    override fun getFileName(): Path = Path.of(resource())

    fun resource(): String = toString().substring(PREFIX.length).let {
        if (it[0] == '/') it.substring(1)
        else it
    }

    fun resourceAsStream(): InputStream? = javaClass.classLoader.getResourceAsStream(resource())

    fun readAllBytes(): ByteArray = resourceAsStream()?.readAllBytes() ?: throw IOException("Error reading $this")

    fun readText(charset: Charset = Charsets.UTF_8): String =
        resourceAsStream()?.bufferedReader(charset)?.readText() ?: throw IOException(
            "Could not load $this. Either the resource could not be found, " +
                "the resource is in a package that is not opened unconditionally, " +
                "or access to the resource is denied by the security manager.")

    fun readAllLines(charset: Charset = Charsets.UTF_8): List<String> =
        resourceAsStream()?.bufferedReader(charset)?.use { reader ->
            val result: MutableList<String> = ArrayList()
            while (true) {
                val line = reader.readLine() ?: break
                result.add(line)
            }
            return@readAllLines result
        } ?: throw IOException("Could not load $this. Either the resource could not be found, " +
            "the resource is in a package that is not opened unconditionally, " +
            "or access to the resource is denied by the security manager.")

    fun copyTo(dest: Path, createDirectories: Boolean = true): Path {
        if (createDirectories) Files.createDirectories(dest.parent)
        Files.write(dest, readAllBytes())
        return dest
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassPath

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String = PREFIX + path
}
