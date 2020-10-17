package com.bkahlert.koodies.nio

import com.bkahlert.koodies.number.times
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalTime
@Execution(CONCURRENT)
internal class NonBlockingCharReaderTest {

    @Test
    internal fun `should read null if empty`(logger: InMemoryLogger<String?>) {
        val reader = NonBlockingCharReader("".byteInputStream(), 100.milliseconds)
        10 times { expectThat(reader.read(CharArray(1), 0, 1, logger)).isEqualTo(-1) }
    }

    @Test
    internal fun `should return null if source is closed`(logger: InMemoryLogger<String?>) {
        val reader = NonBlockingCharReader("123".byteInputStream(), 100.milliseconds)
        expectThat(reader.readText()).isEqualTo("123")
        10 times { expectThat(reader.read(CharArray(1), 0, 1, logger)).isEqualTo(-1) }
    }

    @Test
    internal fun `should read content`(logger: InMemoryLogger<String?>) {
        val line = "line #壹\nline #❷"
        val reader = NonBlockingCharReader(line.byteInputStream(), 100.milliseconds)
        expectThat(reader.readLines()).containsExactly("line #壹", "line #❷")
    }
}
