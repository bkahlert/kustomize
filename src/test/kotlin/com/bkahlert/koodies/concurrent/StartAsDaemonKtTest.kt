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
internal class StartAsDaemonKtTest {
    @OptIn(ExperimentalTime::class)
    @ConcurrentTestFactory
    internal fun `should start daemon`() = listOf(
        "prefix form" to { finished: AtomicBoolean -> { finished.set(Thread.currentThread().isDaemon) }.startAsDaemon() },
        "postfix form" to { finished: AtomicBoolean -> startAsDaemon() { finished.set(Thread.currentThread().isDaemon) } },
    ).map { (caption, exec) ->
        dynamicTest(caption) {
            val isDaemon = AtomicBoolean(false)
            measureTime {
                exec(isDaemon)
                while (!isDaemon.get()) {
                    1.milliseconds.sleep()
                }
            }.let { expectThat(it).isLessThan(100.milliseconds) }
        }
    }

    @OptIn(ExperimentalTime::class)
    @ConcurrentTestFactory
    internal fun `should start immediately`() = listOf(
        "prefix form" to { finished: AtomicBoolean -> { finished.set(true) }.startAsDaemon() },
        "postfix form" to { finished: AtomicBoolean -> startAsDaemon() { finished.set(true) } },
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
        "prefix form" to { finished: AtomicBoolean -> { finished.set(true) }.startAsDaemon(500.milliseconds) },
        "postfix form" to { finished: AtomicBoolean -> startAsDaemon(500.milliseconds) { finished.set(true) } },
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
