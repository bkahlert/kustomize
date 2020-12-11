package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.concurrent.process.CommandLine
import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.process
import com.bkahlert.koodies.concurrent.process.waitForTermination
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.docker.DockerRunAdaptable
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.logging
import com.imgcstmzr.util.isReadable
import com.imgcstmzr.util.isWritable
import java.nio.file.Path

abstract class LibguestfsCommandLine(command: String, arguments: List<String>) :
    CommandLine(redirects = emptyList(), command = command, arguments = arguments),
    DockerRunAdaptable {

    protected open fun executionCaption() = "Running $summary..."

    fun execute(logger: RenderingLogger): Int =
        logger.logging(caption = executionCaption(), ansiCode = ANSI.termColors.brightBlue) {

            val disk: Path = commandLineParts.dropWhile { it != "--add" }.drop(1).take(1).singleOrNull()?.toPath().run {
                checkNotNull(this) { "No included disk found." }
                check(exists) { "Disk $this does no exist." }
                check(isReadable) { "Disk $this is not readable." }
                check(isWritable) { "Disk $this is not writable." }
                this
            }

            val sharedDir = disk.resolveSibling("shared")
            sharedDir.mkdirs()

            LibguestfsProcess(this@LibguestfsCommandLine)
                .process(nonBlockingReader = false) { io ->
                    when (io.type) {
                        IO.Type.META -> logLine { io }
                        IO.Type.IN -> logLine { io }
                        IO.Type.OUT -> logLine { io }
                        IO.Type.ERR -> logLine { "Unfortunately an error occurred: ${io.formatted}" }
                    }
                }
                .waitForTermination()
        }
}

class LibguestfsProcess(libguestCommandLine: LibguestfsCommandLine) :
    DockerProcess(libguestCommandLine)

open class Option(open val name: String, open val arguments: List<String>) :
    List<String> by listOf(name) + arguments {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Option

        if (name != other.name) return false
        if (arguments != other.arguments) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + arguments.hashCode()
        return result
    }

    override fun toString(): String {
        val simpleName = this::class.simpleName ?: "GuestfishCommand"
        val cmd = simpleName.replace("Guestfish", "><> ").replace("Command", "")
        return "$cmd($this)"
    }
}

/**
 * Add a block device or virtual machine image to the shell.
 *
 * The format of the disk image is auto-detected.
 */
interface DiskOption {
    val disk: Path
}
