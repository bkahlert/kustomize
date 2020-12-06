package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.DelegatingProcess
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.time.poll
import java.util.concurrent.TimeoutException
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A [Process] responsible to run a [Docker] container.
 */
open class DockerProcess private constructor(
    override val name: DockerContainerName,
    delegatingProcess: DelegatingProcess,
) : DockerContainer, DelegatingProcess(delegatingProcess) {
    constructor(name: DockerContainerName, commandLine: DockerRunCommandLine) :
        this(name, commandLine.lazyStart(processTerminationCallback = {
            Docker.stop(name)
            Docker.remove(name, forcibly = true)
        }))

    constructor(commandLine: DockerRunCommandLine) :
        this(commandLine.options.name ?: error("Docker container name missing."), commandLine)

    init {
        "🐳 docker attach ${name.sanitized.quoted}".log()
    }

    override fun destroy(): Unit = cleanUp(forcibly = false).also { super.destroy() }
    override fun destroyForcibly(): Process = cleanUp(forcibly = true).let { super.destroyForcibly() }

    override fun toString(): String = super.toString().replaceFirst("Process[", "DockerProcess[name=$name, ")

    private fun cleanUp(forcibly: Boolean) {
        stop() // asynchronous call; just trying to be nice to call stop
        remove(forcibly)
        poll { !isRunning }.every(100.milliseconds).forAtMost(10.seconds) {
            throw TimeoutException("Could not clean up $this within $it.")
        }
    }
}
