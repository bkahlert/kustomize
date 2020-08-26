package com.imgcstmzr.runtime

import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Suppress("RedundantInnerClassModifier")
@Execution(ExecutionMode.CONCURRENT)
internal class FakeProcessTest {

    @Nested
    inner class SlowInputStream {
        @Test
        internal fun `should provide input correctly`() {
            val slowInputStream = FakeProcess.SlowInputStream("Hello\n", "World!\n", delay = Duration.ofSeconds(1))

            assertTimeoutPreemptively(Duration.ofSeconds(10)) {
                val read = String(slowInputStream.readAllBytes())

                expectThat(read).isEqualTo("Hello\nWorld!\n")
            }
        }

        @OptIn(ExperimentalTime::class)
        @Test
        internal fun `should provide input slowly`() {
            val delay = Duration.ofSeconds(1)
            val slowInputStream = FakeProcess.SlowInputStream("Hello\n", "World!\n", delay = delay)

            assertTimeoutPreemptively(delay.multipliedBy(5)) {
                val duration = measureTime {
                    String(slowInputStream.readAllBytes())
                }
                expectThat(duration).assertThat("is slow") { it.inSeconds > delay.seconds }
            }
        }
    }
}
