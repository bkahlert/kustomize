package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.startAsDaemon
import com.bkahlert.koodies.time.poll
import com.bkahlert.koodies.time.sleep
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isNotEmpty
import java.util.concurrent.CompletableFuture
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class RunningProcessTest {
    @Test
    fun `should format CompleteFuture simplified`() {
        val runningProcess: RunningProcess = object : RunningProcess() {
            override val process: Process = nullProcess
            override val result: CompletableFuture<CompletedProcess> = CompletableFuture.completedFuture(CompletedProcess(-1, -1, emptyList()))
            override val ioLog: IOLog = IOLog()
        }

        expectThat(runningProcess.toString()).contains("; Completed normally;")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should provide thread-safe access to log`() {
        var stop = false
        val runningProcess: RunningProcess = object : RunningProcess() {
            override val process: Process get() = nullProcess
            override val result: CompletableFuture<CompletedProcess> = CompletableFuture.completedFuture(CompletedProcess(-1, -1, emptyList()))
            override val ioLog: IOLog = IOLog()

            init {
                startAsDaemon {
                    var i = 0
                    while (!stop) {
                        ioLog.add(IO.Type.META, "being busy $i times\n".toByteArray())
                        10.milliseconds.sleep()
                        i++
                    }
                }
            }
        }

        poll { runningProcess.ioLog.logged.isNotEmpty() }.every(10.milliseconds).forAtMost(1.seconds) { fail("No I/O logged in one second.") }

        expectThat(runningProcess.ioLog.logged) {
            isNotEmpty()
            contains(IO.Type.META typed "being busy 0 times")
        }
        stop = true
    }
}
