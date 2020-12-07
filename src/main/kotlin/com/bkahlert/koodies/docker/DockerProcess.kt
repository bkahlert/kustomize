package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.ManagedProcess
import com.bkahlert.koodies.concurrent.process.Process
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.time.poll
import java.util.concurrent.TimeoutException
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A [Process] responsible to run a [Docker] container.
 */
open class DockerProcess private constructor(
    val name: String,
    commandLine: DockerRunCommandLine,
) : ManagedProcess(commandLine, processTerminationCallback = {
    Docker.stop(name)
    Docker.remove(name, forcibly = true)
}) {

    constructor(commandLine: DockerRunCommandLine) :
        this(commandLine.options.name?.sanitized ?: error("Docker container name missing."), commandLine)

    init {
        start()
        metaLog("🐳 docker attach ${name.quoted}") // TODO consume by Processors
    }

    val isRunning: Boolean get() = Docker.isContainerRunning(name)

    override fun stop(): Process = also { Docker.stop(name) }.also { pollTermination() }.also { super.stop() }
    override fun kill(): Process = also { stop() }.also { super.kill() }

    private fun pollTermination(): DockerProcess {
        poll { !isRunning }.every(100.milliseconds).forAtMost(10.seconds) {
            throw TimeoutException("Could not clean up $this within $it.")
        }
        return this
    }

    override fun toString(): String = super.toString().replaceFirst("Process[", "DockerProcess[name=$name, ")
}
