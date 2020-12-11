package com.imgcstmzr.libguestfs.docker

import com.bkahlert.koodies.builder.OnOffBuilder
import com.bkahlert.koodies.concurrent.process.CommandLine
import com.bkahlert.koodies.docker.DockerImage
import com.bkahlert.koodies.docker.DockerImageBuilder
import com.bkahlert.koodies.docker.DockerRunAdaptable
import com.bkahlert.koodies.docker.DockerRunCommandLine
import com.bkahlert.koodies.docker.DockerRunCommandLineBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.nio.exception.noSuchFile
import com.bkahlert.koodies.string.withRandomSuffix
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption.DiskOption
import com.imgcstmzr.util.isReadable
import java.nio.file.Path

/**
 * Layout:
 * ```
 * some/
 *   ↳ dir/
 *     ↳ $(PWD)
 *       ↳ disk.img
 *       ↳ shared/
 * ```
 *
 * `disk.img` = must be contained in [CommandLine], e.g. by [GuestfishOption.DiskOption]
 *
 * Adapts the given [CommandLine] so that it runs containerized using Docker.
 */
interface LibguestfsDockerAdaptable : DockerRunAdaptable {
    companion object {
        val IMAGE: DockerImage =
            DockerImageBuilder.build { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }
    }

    val command: String
    val arguments: List<String>
    val disks: List<Path>

    fun checkCommand() {
        check(command == command) { "This adapter can only adapt $command commands." }
    }

    fun checkArguments() {
        check(arguments.size > 1) { "The $command command is empty." }
    }

    fun checkDisks(): Path {
        check(disks.size == 1) { "The $command command must add exactly one disk. ${disks.size} found: ${disks.joinToString(", ")}." }
        return disks.single().also { check(it.isReadable) { it.noSuchFile() } }
    }

    override fun adapt(): DockerRunCommandLine {
        checkCommand()
        checkArguments()
        val disk = checkDisks()

        return IMAGE.buildRunCommand {
            redirects { +"2>&1" } // needed since some commandrvf writes all output to stderr
            options {
                entrypoint { command }
                name { "libguestfs-$command".withRandomSuffix() }
                autoCleanup { OnOffBuilder.on }
                mounts {
                    disk.resolveSibling("shared") mountAt "/shared"
                    disk mountAt "/images/disk.img"
                }
            }
            arguments {
                +DiskOption(Path.of("/images/disk.img"))
                +arguments
            }
        }
    }
}
