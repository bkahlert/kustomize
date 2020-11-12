package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.CompletedProcess
import com.bkahlert.koodies.concurrent.process.IOLog
import com.bkahlert.koodies.concurrent.process.RunningProcess
import com.bkahlert.koodies.time.poll
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
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
    override val process: RunningProcess get() = runningProcessProvider()
    override val result: CompletableFuture<CompletedProcess> get() = CompletableFuture.supplyAsync { runningProcessProvider().waitForCompletion() }
    override val ioLog: IOLog get() = runningProcessProvider().ioLog
    override fun destroy(): Unit = cleanUp(forcibly = false).also { super.destroy() }
    override fun destroyForcibly(): Process = cleanUp(forcibly = true).let { super.destroyForcibly() }

    override fun toString(): String = "DockerProcess(name='$name', runningProcess=$process)"

    private fun cleanUp(forcibly: Boolean) {
        stop() // asynchronous call; just trying to be nice to call stop
        remove(forcibly)
        poll { !isRunning }.every(100.milliseconds).forAtMost(10.seconds) {
            throw TimeoutException("Could not clean up $this within $it.")
        }
    }

    companion object {
        val nullDockerProcess: DockerProcess = DockerProcess("NULL") { nullRunningProcess }
    }
}
