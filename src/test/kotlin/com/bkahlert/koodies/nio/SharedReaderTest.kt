package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.joinLinesToString
import com.bkahlert.koodies.test.junit.Slow
import com.imgcstmzr.runtime.ProcessMock
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.notContainsLineSeparator
import com.imgcstmzr.util.prefixes
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
import java.io.InputStream
import java.io.Reader
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalTime::class)
@Disabled
abstract class SharedReaderTest(val readerFactory: (InputStream, Duration, RenderingLogger<String?>?) -> Reader) {

    @Slow
    @RepeatedTest(10)
    fun `should not block`(logger: InMemoryLogger<String?>) {
        val slowInputStream = ProcessMock.SlowInputStream("Hel", "lo\n", "World!\n",
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
        val slowInputStream = ProcessMock.SlowInputStream("𝌪𝌫𝌬𝌭𝌮", "𝌯𝌰\n", "𝌱𝌲𝌳𝌴𝌵\n",
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
        val slowInputStream = ProcessMock.SlowInputStream("Hel", "lo\n\n\n\n\n", "World!\n",
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
        val slowInputStream = ProcessMock.SlowInputStream("Hello\r", "\nWorld",
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
    @ExperimentalTime
    @Nested
    inner class Benchmark {
        val bootLog = ClassPath("raspberry.boot")
        val inputStream = { bootLog.resourceAsStream() ?: throw IllegalStateException() }

        @Slow
        @Test
        fun `should quickly read boot sequence using custom forEachLine`(logger: InMemoryLogger<String?>) {
            val reader = readerFactory(inputStream(), 1.seconds, logger)

            val read = mutableListOf<String>()
            assertTimeoutPreemptively(30.seconds.toJavaDuration()) {
                reader.forEachLine {
                    read.add(it)
                }
            }
            expectThat(read.joinLinesToString()).isEqualTo(inputStream().readAllBytes().decodeToString().withoutTrailingLineSeparator)
        }

        @Slow
        @Test
        fun `should quickly read boot sequence using foreign forEachLine`(logger: InMemoryLogger<String?>) {
            val reader = readerFactory(inputStream(), 1.seconds, logger)

            assertTimeoutPreemptively(30.seconds.toJavaDuration()) {
                val read = reader.readLines()
                expectThat(read.joinLinesToString()).isEqualTo(inputStream().readAllBytes().decodeToString().withoutTrailingLineSeparator)
            }
        }
    }
}
