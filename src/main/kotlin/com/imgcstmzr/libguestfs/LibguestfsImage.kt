package com.imgcstmzr.libguestfs

import koodies.docker.DockerImage

/**
 * Docker image used to run [GuestfishCommandLine] and [VirtCustomizeCommandLine].
 *
 * @see <a href="https://hub.docker.com/repository/docker/bkahlert/libguestfs">bkahlert/libguestfs</a>
 */
object LibguestfsImage : DockerImage("bkahlert", listOf("libguestfs"), digest = "sha256:e8fdf16c69a9155b0e30cdc9b2f872232507f5461be2e7dff307f4c1b50faa20")
