package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.collections.maxOrThrow
import com.bkahlert.koodies.collections.minOrThrow
import com.bkahlert.koodies.concurrent.synchronized
import com.bkahlert.koodies.time.poll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isLessThan
import kotlin.time.milliseconds
import kotlin.time.seconds


@Execution(ExecutionMode.CONCURRENT)
class ConcurrentTest {

    private enum class Tests { TEST1, TEST2, TEST3 }

    private val starts = mutableMapOf<Tests, Long>().synchronized()
    private val ends = mutableMapOf<Tests, Long>().synchronized()

    @TestFactory
    fun `should run concurrent tests`() = Tests.values().map { test ->
        DynamicTest.dynamicTest(test.name) {
            starts[test] = System.currentTimeMillis()
            poll { false }.every(1.seconds).forAtMost(2.seconds) {}
            ends[test] = System.currentTimeMillis()
        }
    }

    @Test
    fun `should run concurrent tests currently`() {
        poll { starts.size == Tests.values().size && ends.size == Tests.values().size }.every(100.milliseconds).indefinitely()
        expectThat(starts.values).get { maxOrThrow() - minOrThrow() }.get { milliseconds }
            .describedAs { "$this difference between starts" }.isLessThan(.5.seconds)
        expectThat(ends.values).get { maxOrThrow() - minOrThrow() }.get { milliseconds }
            .describedAs { "$this difference between ends" }.isLessThan(.5.seconds)
        expectThat(ends.values.maxOrThrow() - starts.values.minOrThrow()).get { milliseconds }
            .describedAs { "$this overall time needed" }.isLessThan(2.5.seconds)
    }
}
