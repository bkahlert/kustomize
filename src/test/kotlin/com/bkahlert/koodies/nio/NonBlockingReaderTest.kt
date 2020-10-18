package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.lines
import com.bkahlert.koodies.test.junit.Slow
import com.imgcstmzr.runtime.ProcessMock
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.notContainsLineSeparator
import com.imgcstmzr.util.prefixes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isEqualTo
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@ExperimentalTime
@Execution(CONCURRENT)
internal class NonBlockingReaderTest {

    @Slow
    @RepeatedTest(10)
    internal fun `should not block`(logger: InMemoryLogger<String?>) {
        val slowInputStream = ProcessMock.SlowInputStream("Hel", "lo\n", "World!\n",
            baseDelayPerInput = 1.seconds,
            logger = logger)
        val reader = NonBlockingReader(slowInputStream, timeout = 5.seconds)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            while (read.lastOrNull() != "World!") {
                val readLine = reader.readLine() ?: return@assertTimeoutPreemptively
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
    internal fun `should read characters that are represented by two chars`(logger: InMemoryLogger<String?>) {
        val slowInputStream = ProcessMock.SlowInputStream("𝌪𝌫𝌬𝌭𝌮", "𝌯𝌰\n", "𝌱𝌲𝌳𝌴𝌵\n",
            baseDelayPerInput = 1.seconds,
            logger = logger)
        val reader = NonBlockingReader(slowInputStream, timeout = .5.seconds, logger = logger)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            while (read.lastOrNull() != "𝌱𝌲𝌳𝌴𝌵") {
                val readLine = reader.readLine() ?: return@assertTimeoutPreemptively
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
    internal fun `should never have trailing line separators`(logger: InMemoryLogger<String?>) {
        val slowInputStream = ProcessMock.SlowInputStream("Hel", "lo\n\n\n\n\n", "World!\n",
            baseDelayPerInput = 1.seconds,
            logger = logger)
        val reader = NonBlockingReader(slowInputStream, timeout = 5.seconds)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).all { notContainsLineSeparator() }
    }

    @Isolated
    @ExperimentalTime
    @Nested
    inner class Benchmark {
        val bootLog = ClassPath("raspberry.boot")
        val inputStream = { bootLog.resourceAsStream() ?: throw IllegalStateException() }

        @Test
        internal fun `should quickly read boot sequence using custom forEachLine`(logger: InMemoryLogger<String?>) {

            val reader = NonBlockingReader(inputStream(), timeout = 1000.milliseconds, logger = logger)

            val read = mutableListOf<String>()
            assertTimeoutPreemptively(8.seconds.toJavaDuration()) {
                reader.forEachLine {
                    read.add(it)
                }
            }
            expectThat(read.lines()).isEqualTo(inputStream().readAllBytes().decodeToString().withoutTrailingLineSeparator)
        }

        @Test
        internal fun `should quickly read boot sequence using foreign forEachLine`(logger: InMemoryLogger<String?>) {
            val reader = NonBlockingReader(inputStream(), timeout = 1000.milliseconds, logger = logger)

            assertTimeoutPreemptively(8.seconds.toJavaDuration()) {
                val read = reader.readLines()
                expectThat(read.lines()).isEqualTo(inputStream().readAllBytes().decodeToString().withoutTrailingLineSeparator)
            }
        }
    }
}
