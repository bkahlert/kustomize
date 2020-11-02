package com.bkahlert.koodies.concurrent.process

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import java.util.concurrent.CompletableFuture

@Execution(CONCURRENT)
class RunningProcessTest {
    @Test
    fun `should format CompleteFuture simplified`() {
        val runningProcess: RunningProcess = object : RunningProcess() {
            override val process: Process get() = nullProcess
            override val result: CompletableFuture<CompletedProcess> = CompletableFuture.completedFuture(CompletedProcess(-1, -1, emptyList()))
        }

        expectThat(runningProcess.toString()).contains("result=Completed normally")
    }
}
