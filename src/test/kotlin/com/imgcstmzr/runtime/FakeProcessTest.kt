package com.imgcstmzr.runtime

import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@Suppress("RedundantInnerClassModifier")
@Execution(ExecutionMode.CONCURRENT)
internal class FakeProcessTest {

    @ExperimentalTime
    @Nested
    inner class SlowInputStream {
        @Test
        internal fun `should provide input correctly`() {
            val slowInputStream = FakeProcess.SlowInputStream("Hello\n", "World!\n", delay = 1.seconds)

            assertTimeoutPreemptively(Duration.ofSeconds(10)) {
                val read = String(slowInputStream.readAllBytes())

                expectThat(read).isEqualTo("Hello\nWorld!\n")
            }
        }

        @OptIn(ExperimentalTime::class)
        @Test
        internal fun `should provide input slowly`() {
            val delay = 1.seconds
            val slowInputStream = FakeProcess.SlowInputStream("Hello\n", "World!\n", delay = delay)

            assertTimeoutPreemptively((delay * 5).toJavaDuration()) {
                val duration = measureTime {
                    String(slowInputStream.readAllBytes())
                }
                expectThat(duration).assertThat("is slow") { it.inSeconds > delay.toLong(TimeUnit.SECONDS) }
            }
        }
    }
}
