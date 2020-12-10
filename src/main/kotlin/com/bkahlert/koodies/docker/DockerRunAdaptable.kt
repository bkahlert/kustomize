package com.bkahlert.koodies.docker

interface DockerRunAdaptable {
    fun adapt(): DockerRunCommandLine
}
