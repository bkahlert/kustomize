@file:Suppress("SpellCheckingInspection")

package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.builder.OnOffBuilder.on
import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.process
import com.bkahlert.koodies.concurrent.process.waitForTermination
import com.bkahlert.koodies.docker.DockerImage
import com.bkahlert.koodies.docker.DockerImageBuilder
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.docker.DockerRunCommandLine
import com.bkahlert.koodies.docker.DockerRunCommandLineBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.nio.exception.noSuchFile
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.subLogger
import java.nio.file.Path
import kotlin.io.path.isReadable

fun libguestfs(init: VirtCustomizeCommandLine.OptionsBuilder.() -> Unit) = com.imgcstmzr.libguestfs.VirtCustomizeCommandLine(init)

class LibguestfsProcess(
    commandLine: VirtCustomizeCommandLine,
    val logger: RenderingLogger,
) : DockerProcess(LibguestfsRunner.adapt(commandLine))


fun VirtCustomizeCommandLine.execute(
    logger: RenderingLogger,
): Int = logger.subLogger("Running $commandLine", ansiCode = ANSI.termColors.blue) {

    logLine { "Preparing shared folder" }
    options.filterIsInstance<DiskOption>().forEach { diskOption ->
        val disk = diskOption.disk.toPath()
        disk.requireExists()
        val sharedDir = disk.resolveSibling("shared")
        sharedDir.mkdirs()
    }

    val libguestfsProcess = LibguestfsProcess(this@execute, this@subLogger)
    libguestfsProcess.process(nonBlockingReader = false) { io ->
        when (io.type) {
            IO.Type.META -> logLine { io }
            IO.Type.IN -> logLine { io }
            IO.Type.OUT -> logLine { io }
            IO.Type.ERR -> logLine { "Unfortunately an error occurred: ${io.formatted}" }
        }
    }.waitForTermination()
}


object LibguestfsRunner {

    val image: DockerImage =
        DockerImageBuilder.build { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }

    const val VirtCustomizeCommandName = "virt-customize"

    /**
     * Layout:
     * ```
     * some/
     *   ↳ dir/
     *     ↳ $(PWD)
     *       ↳ the-image.img
     *       ↳ shared/
     * ```
     *
     * `the-image.img` = must be contained in [commandLine], e.g. by [DiskOption]
     *
     * Adapts the given [VirtCustomizeCommandLine] so that it runs containerized using TODO
     */
    fun adapt(commandLine: VirtCustomizeCommandLine): DockerRunCommandLine {
        val commandName = VirtCustomizeCommandName
        check(commandLine.command == VirtCustomizeCommandName) { "The $commandName command is empty. Skipping." }
        if (commandLine.options.isEmpty()) throw NoSuchElementException("The $commandName command is empty. Skipping.")

        val (diskOptions, nonDiskOptions) = commandLine.options
            .partition { it is DiskOption }
            .let { (diskOptions, nonDiskOptions) -> diskOptions.filterIsInstance<DiskOption>() to nonDiskOptions }

        val disk = diskOptions.singleOrNull()?.path
        checkNotNull(disk) { "The $commandName command must add exactly one disk. ${diskOptions.size} found." }

        if (!disk.isReadable()) throw noSuchFile(disk)

        return image.buildRunCommand {
            redirects { +"2>&1" } // needed since some commandrvf writes all output to stderr
            options {
                entrypoint { commandLine.command }
                name { "libguestfs" }
                autoCleanup { on }
                mounts {
                    disk.resolveSibling("shared") mountAt "/shared"
                    disk mountAt "/images/disk.img"
                }
            }
            arguments {
                +DiskOption(Path.of("/images/disk.img"))
                nonDiskOptions.forEach { +it }
            }
        }
    }
}
