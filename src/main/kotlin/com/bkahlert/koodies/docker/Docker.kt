package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.Processes.checkIfOutputContains
import com.bkahlert.koodies.concurrent.process.Processes.evalShellScript
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.time.sleep
import java.util.concurrent.TimeUnit
import kotlin.time.seconds

/**
 * Provides methods to create and interact with a [DockerProcess].
 */
object Docker {
    /**
     * Whether the Docker engine itself is running.
     */
    val isEngineRunning: Boolean get() = !checkIfOutputContains("docker info", "error")

    /**
     * Whether a Docker container with the given [name] is running.
     */
    fun isContainerRunning(name: String): Boolean = name.let { sanitizedName ->
        checkIfOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$sanitizedName${'$'}"""", sanitizedName)
    }

    /**
     * Whether a Docker container—no matter if it's running or not—exists.
     */
    fun exists(name: String): Boolean = name.let { sanitizedName ->
        checkIfOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$sanitizedName${'$'}" --all""", sanitizedName)
    }

    /**
     * Extends [ShellScript] with an entry point to build docker commands.
     */
    fun image(init: DockerImageBuilder.() -> Any): DockerRunCommandLineBuilder.ImageProvidedBuilder =
        DockerRunCommandLineBuilder.ImageProvidedBuilder(DockerImageBuilder.build(init))

    /**
     * Explicitly stops the Docker container with the given [name] **asynchronously**.
     */
    fun stop(name: String) {
        evalShellScript { !"docker stop \"$name\"" }.onExit.orTimeout(8, TimeUnit.SECONDS)
        1.seconds.sleep()
    }

    /**
     * Explicitly (stops and) removes the Docker container with the given [name] **synchronously**.
     *
     * If needed even [forcibly].
     */
    fun remove(name: String, forcibly: Boolean = false) {
        val forceOption = if (forcibly) " --force" else ""
        evalShellScript { !"docker rm$forceOption \"$name\"" }.onExit.orTimeout(8, TimeUnit.SECONDS)
        1.seconds.sleep()
    }

}

