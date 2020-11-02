package com.bkahlert.koodies.concurrent

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.util.concurrent.atomic.AtomicLong

@Execution(CONCURRENT)
class StartAsCoroutineKtTest {
    @Test
    fun `should run`() {
        println("Start")

// Start a coroutine
        val c = AtomicLong()

        for (i in 1..1_000_000L)
            GlobalScope.launch {
                c.addAndGet(i)
            }

        println(c.get())
    }
}
