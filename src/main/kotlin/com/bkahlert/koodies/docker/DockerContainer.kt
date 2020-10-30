package com.bkahlert.koodies.docker

/**
 * A [Docker] container identified by its name.
 */
interface DockerContainer {
    /**
     * Name of this [Docker] container.
     */
    val name: String

    /**
     * Whether the Docker container is currently running.
     */
    val isRunning: Boolean get() = Docker.isContainerRunning(name)

    fun stop() = Docker.stop(name)

    fun remove(forcibly: Boolean = false) = Docker.remove(name, forcibly)
}
