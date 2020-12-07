package com.bkahlert.koodies.concurrent

import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan

@Execution(CONCURRENT)
class BusyThreadTest {
    @Test
    fun `should not stop until asked`(tracer: InMemoryLogger) {
        val thread = BusyThread(tracer)

        val start = System.currentTimeMillis()
        while (thread.isAlive) {
            if (System.currentTimeMillis() - start > 2000) {
                thread.stopFussFree()
            }
        }

        expectThat(System.currentTimeMillis() - start).isGreaterThan(2000)
    }
}
