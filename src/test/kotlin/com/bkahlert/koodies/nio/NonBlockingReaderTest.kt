package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.lines
import com.imgcstmzr.runtime.ProcessMock
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.prefixes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isEqualTo
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@Execution(CONCURRENT)
internal class NonBlockingReaderTest {
    @ExperimentalTime
    @Timeout(120, unit = TimeUnit.SECONDS)
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

    @ExperimentalTime
    @Timeout(120, unit = TimeUnit.SECONDS)
    @RepeatedTest(10)
    internal fun `should non-BEM character block`(logger: InMemoryLogger<String?>) {
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
            assertTimeoutPreemptively(10.seconds.toJavaDuration()) {
                reader.forEachLine { read.add(it) }
            }
            expectThat(read.lines()).isEqualTo(inputStream().readAllBytes().decodeToString().withoutTrailingLineSeparator)
        }

        @Test
        internal fun `should quickly read boot sequence using foreign forEachLine`(logger: InMemoryLogger<String?>) {
            val reader = NonBlockingReader(inputStream(), timeout = 1000.milliseconds, logger = logger)

            assertTimeoutPreemptively(10.seconds.toJavaDuration()) {
                val read = reader.readLines()
                expectThat(read.lines()).isEqualTo(inputStream().readAllBytes().decodeToString().withoutTrailingLineSeparator)
            }
        }
    }
}
