package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.time.sleep
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.milliseconds

@Execution(CONCURRENT)
internal class StartAsThreadKtTest {

    // can't check if not daemon as it might be inherited from parent thread

    @OptIn(ExperimentalTime::class)
    @ConcurrentTestFactory
    internal fun `should start immediately`() = listOf(
        "prefix form" to { finished: AtomicBoolean -> { finished.set(true) }.startAsThread() },
        "postfix form" to { finished: AtomicBoolean -> startAsThread() { finished.set(true) } },
    ).map { (caption, exec) ->
        dynamicTest(caption) {
            val finished = AtomicBoolean(false)
            measureTime {
                exec(finished)
                while (!finished.get()) {
                    1.milliseconds.sleep()
                }
            }.let { expectThat(it).isLessThan(100.milliseconds) }
        }
    }

    @OptIn(ExperimentalTime::class)
    @ConcurrentTestFactory
    internal fun `should start delayed`() = listOf(
        "prefix form" to { finished: AtomicBoolean -> { finished.set(true) }.startAsThread(500.milliseconds) },
        "postfix form" to { finished: AtomicBoolean -> startAsThread(500.milliseconds) { finished.set(true) } },
    ).map { (caption, exec) ->
        dynamicTest(caption) {
            val finished = AtomicBoolean(false)
            measureTime {
                exec(finished)
                while (!finished.get()) {
                    1.milliseconds.sleep()
                }
            }.let { expectThat(it).isGreaterThan(400.milliseconds).isLessThan(600.milliseconds) }
        }
    }
}
