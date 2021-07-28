package com.bkahlert.koodies.nio

import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.nio.file.classPath
import com.bkahlert.koodies.nio.file.inputStream
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.fuzzyLevenshteinDistance
import com.bkahlert.koodies.string.joinLinesToString
import com.bkahlert.koodies.test.junit.Slow
import com.imgcstmzr.runtime.SlowInputStream
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.notContainsLineSeparator
import com.imgcstmzr.util.prefixes
import org.apache.commons.io.input.TeeInputStream
import org.apache.commons.io.output.ByteArrayOutputStream
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import java.io.InputStream
import java.io.Reader
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@Disabled
abstract class SharedReaderTest(val readerFactory: (InputStream, Duration, RenderingLogger<*>) -> Reader) {

    @Slow
    @RepeatedTest(3)
    fun `should not block`(logger: InMemoryLogger<String?>) {
        val slowInputStream = SlowInputStream("Hel", "lo\n", "World!\n",
            baseDelayPerInput = 1.seconds,
            logger = logger)
        val reader = readerFactory(slowInputStream, 5.seconds, logger)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            while (read.lastOrNull() != "World!") {
                val readLine = (reader as? NonBlockingReader)?.readLine() ?: return@assertTimeoutPreemptively
                read.add(readLine)
            }
        }

        // e.g. [, , , , Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hello, , , , , , , , , , World, World!]
        expectThat(read
            .takeWhile { it != "Hello" }
        ).all { prefixes("Hello") }
        expectThat(read
            .dropWhile { it != "Hello" }
            .filter { it.isNotBlank() }
            .takeWhile { it == "Hello" }
        ).all { isEqualTo("Hello") }
        expectThat(read
            .dropWhile { it != "Hello" }
            .filter { it.isNotBlank() }
            .dropWhile { it == "Hello" }
            .filter { it.isNotBlank() }
        ).all { prefixes("World!") }
    }

    @Slow
    @RepeatedTest(3)
    fun `should read characters that are represented by two chars`(logger: InMemoryLogger<String?>) {
        val slowInputStream = SlowInputStream("𝌪𝌫𝌬𝌭𝌮", "𝌯𝌰\n", "𝌱𝌲𝌳𝌴𝌵\n",
            baseDelayPerInput = 1.seconds,
            logger = logger)
        val reader = readerFactory(slowInputStream, .5.seconds, logger)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            while (read.lastOrNull() != "𝌱𝌲𝌳𝌴𝌵") {
                val readLine = (reader as? NonBlockingReader)?.readLine() ?: return@assertTimeoutPreemptively
                read.add(readLine)
            }
        }

        // e.g. [, , , , Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hello, , , , , , , , , , World, World!]
        expectThat(read
            .takeWhile { it != "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
        ).all { prefixes("𝌯𝌰") }
        expectThat(read
            .dropWhile { it != "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
            .filter { it.isNotBlank() }
            .takeWhile { it == "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
        ).all { isEqualTo("𝌪𝌫𝌬𝌭𝌮𝌯𝌰") }
        expectThat(read
            .dropWhile { it != "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
            .filter { it.isNotBlank() }
            .dropWhile { it == "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
            .filter { it.isNotBlank() }
        ).all { prefixes("𝌱𝌲𝌳𝌴𝌵") }
        expectThat("𝌱𝌲𝌳𝌴𝌵")
    }

    @Test
    fun `should never have trailing line separators`(logger: InMemoryLogger<String?>) {
        val slowInputStream = SlowInputStream("Hel", "lo\n\n\n\n\n", "World!\n",
            baseDelayPerInput = 1.seconds,
            logger = logger)
        val reader = readerFactory(slowInputStream, 5.seconds, logger)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).all { notContainsLineSeparator() }
    }


    @Test
    fun `should not repeat line on split CRLF`(logger: InMemoryLogger<String?>) {
        val slowInputStream = SlowInputStream("Hello\r", "\nWorld",
            baseDelayPerInput = 1.seconds,
            logger = logger)
        val reader = readerFactory(slowInputStream, 5.seconds, logger)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).containsExactly("Hello", "World")
    }

    @Suppress("unused")
    @Isolated
    @Nested
    inner class Benchmark {
        private val inputStream = { classPath("raspberry.boot") { inputStream() } ?: throw IllegalStateException() }
        private val expected = inputStream().bufferedReader().readText().withoutTrailingLineSeparator

        @Slow @Test
        fun `should quickly read boot sequence using custom forEachLine`(logger: InMemoryLogger<String?>) {
            val reader = readerFactory(inputStream(), 1.seconds, logger)

            val read = mutableListOf<String>()
            kotlin.runCatching {
                assertTimeoutPreemptively(30.seconds.toJavaDuration()) {
                    reader.forEachLine {
                        read.add(it)
                    }
                }
            }.onFailure { dump("Test failed.") { read.joinLinesToString() } }

            expectThat(read.joinLinesToString()).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
        }

        @Slow @Test
        fun `should quickly read boot sequence using foreign forEachLine`(logger: InMemoryLogger<String?>) {
            val read = ByteArrayOutputStream()
            val reader = readerFactory(TeeInputStream(inputStream(), read), 1.seconds, logger)

            kotlin.runCatching {
                assertTimeoutPreemptively(30.seconds.toJavaDuration()) {
                    val readLines = reader.readLines()
                    expectThat(readLines.joinLinesToString()).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
                }
            }.onFailure { dump("Test failed.") { read.toString(Charsets.UTF_8) } }
        }
    }
}
