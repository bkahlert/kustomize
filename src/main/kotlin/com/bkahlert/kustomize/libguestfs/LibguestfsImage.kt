package com.bkahlert.kustomize.libguestfs

import koodies.docker.DockerImage

/**
 * Docker image used to run [GuestfishCommandLine] and [VirtCustomizeCommandLine].
 *
 * @see <a href="https://hub.docker.com/repository/docker/bkahlert/libguestfs">bkahlert/libguestfs</a>
 */
object LibguestfsImage : DockerImage("bkahlert", listOf("libguestfs"), digest = "sha256:de20843ae800c12a8b498c10ec27e2136b55dee4d62d927dff6b3ae360676d00")
