package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.random
import com.imgcstmzr.runtime.ProcessMock
import com.imgcstmzr.util.logging.InMemoryLogger
import org.jline.utils.InputStreamReader
import org.jline.utils.NonBlocking
import org.jline.utils.NonBlockingReader
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.Reader
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


/**
 * Tests mainly JLine functionality / findings / working patterns.
 */
@ExperimentalTime
@Execution(ExecutionMode.CONCURRENT)
class NonBlockingTest {

    @Nested
    inner class NonBlockingInputStream {
        @Test
        internal fun `should produce same byte sequence as ByteArrayInputStream`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val nonBlockingInputStream =
                NonBlocking.nonBlocking(::`should produce same byte sequence as ByteArrayInputStream`.toString(), input.byteInputStream())
            expectThat(input.byteInputStream().readAllBytes()).isEqualTo(nonBlockingInputStream.readAllBytes())
        }
    }

    @Nested
    inner class NonBlockingInputStreamReader {
        @Test
        internal fun `should produce same byte sequence as ByteArrayInputStreamReader`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val linesExpected = input.byteInputStream().reader(Charsets.UTF_8).readLines()
            val linesActual = NonBlocking.nonBlocking(::`should produce same byte sequence as ByteArrayInputStreamReader`.toString(),
                input.byteInputStream(),
                Charsets.UTF_8).readLines()
            expectThat(linesExpected).isEqualTo(linesActual)
        }

        @ExperimentalTime
        @Test
        internal fun `should read no non-BEM unicode extremely slow input streams`(logger: InMemoryLogger<String?>) {
            val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
            val inputStream = ProcessMock.SlowInputStream(0.seconds to input, baseDelayPerInput = 1.seconds, logger = logger)
            val reader = NonBlocking.nonBlocking(String.random(), NonBlocking.nonBlocking(String.random(), BufferedInputStream(inputStream)), Charsets.UTF_8)
            val readLines = reader.readLines()
            expectThat(readLines).isEqualTo(listOf("A\uD834\uDF2A", "\uD834\uDF2B", "\uD834\uDF2C\uD834\uDF2D\uD834\uDF2E", "Z"))
        }

        @ExperimentalTime
        @Test
        internal fun `should read no non-BEM unicode extremely slow input streams if buffered`(logger: InMemoryLogger<String?>) {
            val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
            val inputStream = ProcessMock.SlowInputStream(0.seconds to input, baseDelayPerInput = 1.seconds, logger = logger)
            val reader =
                NonBlocking.nonBlocking(::`should read no non-BEM unicode extremely slow input streams if buffered`.toString(),
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)))
            val readLines = reader.readLines()
            expectThat(readLines).isEqualTo(listOf("A\uD834\uDF2A", "\uD834\uDF2B", "\uD834\uDF2C\uD834\uDF2D\uD834\uDF2E", "Z"))
        }

        @ExperimentalTime
        @Test
        internal fun `should be equally readable like any other byte input stream`(logger: InMemoryLogger<String?>) {
            val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
            val inputStream = ProcessMock.SlowInputStream(0.seconds to input, baseDelayPerInput = 10.seconds, logger = logger)

            val readFromSlowInput =
                NonBlocking.nonBlocking(String.random(), BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))).readAll(2.seconds)
            val readFromAnyInput = input.byteInputStream().readAllBytes().decodeToString()
            expectThat(readFromSlowInput).isEqualTo(readFromAnyInput).isEqualTo(input)
        }
    }

    fun Reader.read(timeout: Duration = 6.seconds): Int = if (this is NonBlockingReader) this.read(timeout.toLongMilliseconds()) else this.read()

    fun Reader.readAll(timeout: Duration = 6.seconds): String {
        val buffer = StringBuilder()
        kotlin.runCatching {
            var read: Int
            while (read(timeout).also { read = it } > -1) {
                buffer.append(read.toChar())
            }
            buffer.toString()
        }.getOrThrow()
        return buffer.toString()
    }
}
