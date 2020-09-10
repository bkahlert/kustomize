package com.imgcstmzr.util

import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList

class ClassPath private constructor(val path: String) : Path by Path.of("$SCHEMA:$path") {
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

    fun readAllLines(charset: Charset = StandardCharsets.UTF_8): List<String> =
        Files.newBufferedReader(this, charset).use { reader ->
            val result: MutableList<String> = ArrayList()
            while (true) {
                val line = reader.readLine() ?: break
                result.add(line)
            }
            return@readAllLines result
        }

    fun copyTo(dest: Path, createDirectories: Boolean = true): Path {
        if (createDirectories) Files.createDirectories(dest.parent)
        Files.write(dest, readAllBytes())
        return dest
    }

    override fun toString(): String = PREFIX + path
}
