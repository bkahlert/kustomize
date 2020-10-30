package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.time.sleep
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.milliseconds

@Execution(CONCURRENT)
class StartAsCompletableFutureKtTest {
    @OptIn(ExperimentalTime::class)
    @ConcurrentTestFactory
    fun `should start immediately`() = listOf(
        "prefix form" to { finished: AtomicBoolean -> { finished.set(true) }.startAsCompletableFuture() },
        "postfix form" to { finished: AtomicBoolean -> startAsCompletableFuture() { finished.set(true) } },
    ).map { (caption, exec) ->
        DynamicTest.dynamicTest(caption) {
            val finished = AtomicBoolean(false)
            measureTime {
                exec(finished)
                while (!finished.get()) {
                    1.milliseconds.sleep()
                }
            }.let { expectThat(it).isLessThan(80.milliseconds) }
        }
    }

    @OptIn(ExperimentalTime::class)
    @ConcurrentTestFactory
    fun `should start delayed`() = listOf(
        "prefix form" to { finished: AtomicBoolean -> { finished.set(true) }.startAsCompletableFuture(500.milliseconds) },
        "postfix form" to { finished: AtomicBoolean -> startAsCompletableFuture(500.milliseconds) { finished.set(true) } },
    ).map { (caption, exec) ->
        DynamicTest.dynamicTest(caption) {
            val finished = AtomicBoolean(false)
            measureTime {
                exec(finished)
                while (!finished.get()) {
                    1.milliseconds.sleep()
                }
            }.let { expectThat(it).isGreaterThan(400.milliseconds).isLessThan(600.milliseconds) }
        }
    }

    @OptIn(ExperimentalTime::class)
    @ConcurrentTestFactory
    fun `should block until value is returned`() = listOf(
        "prefix form" to { finished: AtomicBoolean -> { finished.set(true); "Hello World" }.startAsCompletableFuture() },
        "postfix form" to { finished: AtomicBoolean -> startAsCompletableFuture { finished.set(true); "Hello World" } },
    ).map { (caption: String, exec: (AtomicBoolean) -> CompletableFuture<String>) ->
        DynamicTest.dynamicTest(caption) {
            val finished = AtomicBoolean(false)
            val value = exec(finished).get()
            expectThat(value).isEqualTo("Hello World")
        }
    }
}
