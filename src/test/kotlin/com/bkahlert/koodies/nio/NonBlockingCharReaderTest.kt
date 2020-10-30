package com.bkahlert.koodies.nio

import com.bkahlert.koodies.number.times
import com.bkahlert.koodies.terminal.ansi.AnsiColors.green
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.terminal.ascii.Borders.SpikedOutward
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.bkahlert.koodies.test.junit.Slow
import com.imgcstmzr.runtime.ProcessMock
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.opentest4j.AssertionFailedError
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@ExperimentalTime
@Execution(CONCURRENT)
internal class NonBlockingCharReaderTest {

    @Test
    internal fun `should read null if empty`(logger: InMemoryLogger<String?>) {
        val reader = NonBlockingCharReader("".byteInputStream(), 100.milliseconds)
        5 times { expectThat(reader.read(CharArray(1), 0, 1, logger)).isLessThanOrEqualTo(0) }
        10 times { expectThat(reader.read(CharArray(1), 0, 1, logger)).isEqualTo(-1) }
    }

    @Test
    internal fun `should return null if source is closed`(logger: InMemoryLogger<String?>) {
        val reader = NonBlockingCharReader("123".byteInputStream(), 100.milliseconds)
        expectThat(reader.readText()).isEqualTo("123")
        5 times { expectThat(reader.read(CharArray(1), 0, 1, logger)).isLessThanOrEqualTo(0) }
        10 times { expectThat(reader.read(CharArray(1), 0, 1, logger)).isEqualTo(-1) }
    }

    @Test
    internal fun `should read content`() {
        val line = "line #壹\nline #❷"
        val reader = NonBlockingCharReader(line.byteInputStream(), 100.milliseconds)
        expectThat(reader.readLines()).containsExactly("line #壹", "line #❷")
    }

    @Slow
    @Test
    internal fun `should read in a non-greedy fashion resp just as much as needed to avoid blocking`(logger: InMemoryLogger<String?>) {
        val inputStream =
            ProcessMock.SlowInputStream(
                1.seconds to "123",
                2.seconds to "abc",
                3.seconds to "!§\"",
                baseDelayPerInput = 0.seconds,
                logger = logger)
        val reader = NonBlockingCharReader(inputStream, 2.seconds)

        kotlin.runCatching {
            expectThat(reader.readLines()).containsExactly("123abc!\"")
                .get { this[0] }.not { contains("$") } // needs a wrapper like NonBlockingReader for characters of length > 1 byte
        }.recover {
            if (it is AssertionFailedError) throw it
            fail(listOf("An exception has occurred while reading the input stream.",
                "Please make sure you don't use a greedy implementation like",
                java.io.InputStreamReader::class.qualifiedName?.magenta() + ".",
                "\nTheir reading strategy blocks the execution leaving you with nothing but timeouts and exceptions.",
                org.jline.utils.InputStreamReader::class.qualifiedName?.green() + "",
                " is known to be a working non-greedy implementation.")
                .wrapWithBorder(SpikedOutward), it)
        }
    }
}
