package com.bkahlert.koodies.time

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
@Execution(CONCURRENT)
class PollTestKt {
    @Test
    fun `should complete if condition becomes true`() {
        var counter = Random(12).nextInt(5, 10)
        expectThat(measureTime { 100.milliseconds.poll { --counter <= 0 }.indefinitely() })
            .isLessThan(1.5.seconds)
    }

    @Test
    fun `should complete if time is up`() {
        val timePassed = measureTime { 100.milliseconds.poll { false }.forAtMost(1.seconds) {} }
        expectThat(timePassed).isGreaterThan(800.milliseconds).isLessThan(1200.milliseconds)
    }

    @Test
    fun `should not call callback if condition becomes true`() {
        var callbackCalled = false
        100.milliseconds.poll { true }.forAtMost(1.seconds) { callbackCalled = true }
        expectThat(callbackCalled).isFalse()
    }

    @Test
    fun `should call callback if time is up`() {
        var callbackCalled = false
        100.milliseconds.poll { false }.forAtMost(1.seconds) { callbackCalled = true }
        expectThat(callbackCalled).isTrue()
    }

    @Test
    fun `should call callback if with actual passed time`() {
        val timeout = 1.seconds
        var timePassed: Duration? = null
        100.milliseconds.poll { false }.forAtMost(timeout) { timePassed = it }
        expectThat(timePassed).isNotNull().isGreaterThan(timeout)
    }

    @Test
    fun `should check condition once per interval`() {
        var counter = 0
        100.milliseconds.poll { counter++; false }.forAtMost(1.seconds) {}
        expectThat(counter).isGreaterThan(5).isLessThan(15)
    }

    @Test
    fun `should only accept positive interval`() {
        expectCatching { measureTime { 0.milliseconds.poll { true } } }
            .isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should poll using alternative syntax`() {
        var counter = 0
        poll { counter++; false }.every(100.milliseconds).forAtMost(1.seconds) {}
        expectThat(counter).isGreaterThan(5).isLessThan(15)
    }
}
