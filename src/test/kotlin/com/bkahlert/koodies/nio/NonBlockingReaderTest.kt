package com.bkahlert.koodies.nio

import com.imgcstmzr.runtime.ProcessMock
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.prefixes
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
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
    @RepeatedTest(100)
    internal fun `should not block`(logger: InMemoryLogger<String?>) {
        val slowInputStream = ProcessMock.SlowInputStream("Hel", "lo\n", "World!\n",
            baseDelayPerWord = 1.seconds,
            logger = logger)
        val reader = NonBlockingReader(slowInputStream, timeout = 100.milliseconds)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            while (read.lastOrNull() != "World!") {
                reader.readLine()?.let { read.add(it) }
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
        expectThat("World!")
    }
}
