package com.bkahlert.koodies.time

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
@Execution(CONCURRENT)
internal class PollTest {
    @Test
    internal fun `should complete if condition is true`() {
        var counter = Random(12).nextInt(5, 10)
        expectThat(measureTime { 100.milliseconds.poll { --counter <= 0 }.indefinitely() })
            .isLessThan(1.5.seconds)
    }

    @Test
    internal fun `should complete if time is up`() {
        expectThat(measureTime { 100.milliseconds.poll { false }.forAtMost(1.seconds) })
            .isGreaterThan(800.milliseconds).isLessThan(1200.milliseconds)
    }

    @Test
    internal fun `should check condition once per interval`() {
        var counter = 0
        100.milliseconds.poll { counter++; false }.forAtMost(1.seconds)
        expectThat(counter).isGreaterThan(5).isLessThan(15)
    }

    @Test
    internal fun `should only accept positive interval`() {
        expectCatching { measureTime { 0.milliseconds.poll { true } } }
            .isFailure().isA<IllegalArgumentException>()
    }
}
