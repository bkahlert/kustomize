package com.bkahlert.koodies.concurrent

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.atomic.AtomicLong

@Execution(CONCURRENT)
class StartAsCoroutineKtTest {
    @Test
    fun `should start coroutine`() {
        val sum = runBlocking {
            val sum = AtomicLong()
            startAsCoroutine {
                for (i in 1..1_000L)
                    launch {
                        sum.addAndGet(i)
                    }
            }.join()
            sum.get()
        }
        expectThat(sum).isEqualTo(500500)
    }
}
