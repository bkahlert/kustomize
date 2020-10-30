package com.bkahlert.koodies.docker

import com.bkahlert.koodies.time.poll
import com.imgcstmzr.process.CompletedProcess
import com.imgcstmzr.process.RunningProcess
import java.util.concurrent.CompletableFuture
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A [Process] responsible to run a [Docker] container.
 */
class DockerProcess(
    /**
     * Name of the [Docker] container.
     */
    override val name: String,
    private val runningProcessProvider: () -> RunningProcess,
) : DockerContainer, RunningProcess() {
    override val process: Process get() = runningProcessProvider()
    override val result: CompletableFuture<CompletedProcess> get() = CompletableFuture.supplyAsync { runningProcessProvider().waitForCompletion() }
    override fun destroy(): Unit = cleanUp(forcibly = false).also { super.destroy() }
    override fun destroyForcibly(): Process = cleanUp(forcibly = true).let { super.destroyForcibly() }

    override fun toString(): String = "DockerProcess(name='$name', runningProcess=$process)"

    @OptIn(ExperimentalTime::class)
    private fun cleanUp(forcibly: Boolean) {
        stop()
        remove(forcibly)
        100.milliseconds.poll { !isRunning }.forAtMost(10.seconds)
    }
}
