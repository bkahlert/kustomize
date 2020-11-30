package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.LoggingProcess
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.time.poll
import java.nio.file.Path
import java.util.concurrent.TimeoutException
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A [Process] responsible to run a [Docker] container.
 */
class DockerProcess private constructor(
    override val name: DockerContainerName,
    command: DockerRunCommand,
    workingDirectory: Path = Paths.WorkingDirectory,
    ioProcessor: (DockerProcess.(IO) -> Unit)? = null,
) : DockerContainer, LoggingProcess(
    command = command,
    workingDirectory = workingDirectory,
    ioProcessor = ioProcessor?.let { { output -> it(this as DockerProcess, output) } },
    runAfterProcessTermination = {
        Docker.stop(name)
        Docker.remove(name, forcibly = true)
    },
) {
    constructor(
        command: DockerRunCommand,
        workingDirectory: Path = Paths.WorkingDirectory,
        ioProcessor: (DockerProcess.(IO) -> Unit)? = null,
    ) : this(command.options.name ?: error("Docker container name missing."), command, workingDirectory, ioProcessor)

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
