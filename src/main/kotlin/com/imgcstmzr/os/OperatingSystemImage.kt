package com.imgcstmzr.os

import com.imgcstmzr.libguestfs.GuestfishCommandLine
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder.GuestfishCommandsContext
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.imgcstmzr.os.OperatingSystem.Credentials
import koodies.builder.Init
import koodies.docker.DockerExec
import koodies.exec.IO
import koodies.io.path.duplicate
import koodies.io.path.getSize
import koodies.io.path.pathString
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.runtime.isDebugging
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs
import koodies.unit.Mega
import koodies.unit.Size
import koodies.unit.bytes
import java.nio.file.Path
import kotlin.io.path.appendBytes
import kotlin.io.path.createDirectories
import kotlin.io.path.isReadable

/**
 * An [path] holding an [operatingSystem] that can be accessed using the specified [credentials].
 */
class OperatingSystemImage(
    val operatingSystem: OperatingSystem,
    /**
     * The credentials that give access to the [OperatingSystem].
     */
    var credentials: Credentials = operatingSystem.defaultCredentials,
    private val path: Path,
) : OperatingSystem by operatingSystem {

    override val fullName: String get() = "${operatingSystem.fullName}${if (updatedCredentials) "*".ansi.magenta else ""} ／ ${path.toUri()}"
    val shortName: String get() = "${operatingSystem.fullName}${if (updatedCredentials) "*".ansi.magenta else ""} ／ ${path.fileName}"
    val fileName: String = path.fileName.pathString
    val readable: Boolean get() = path.isReadable()
    val directory: Path get() = path.parent
    val file: Path get() = path
    val size: Size get() = path.getSize()
    fun duplicate(): OperatingSystemImage = operatingSystem based (path.duplicate())

    private val updatedCredentials: Boolean get() = credentials != defaultCredentials

    /**
     * Contains the directory used to exchange with this image.
     *
     * Example: For `$HOME/.imgcstmzr/project/os.img` the exchange directory
     * would be `$HOME/.imgcstmzr/project/shared`.
     */
    val exchangeDirectory: Path get() = mountRootForDisk(file)

    /**
     * Returns the given [diskPath] mapped to its location in the [exchangeDirectory].
     *
     * Example: For `/var/file` the mapped location would be
     * `$HOME/.imgcstmzr/project/shared/var/file`.
     */
    fun hostPath(diskPath: DiskPath): Path = diskPath.hostPath(exchangeDirectory)

    fun copyOut(path: String, vararg paths: String, logger: FixedWidthRenderingLogger? = BACKGROUND, trace: Boolean = false): Path =
        (logger ?: MutedRenderingLogger).logging("Copying out ${path.formattedAs.input}" + if (paths.isNotEmpty()) " and ${paths.size} other files" else "") {
            guestfish(MutedRenderingLogger, trace) {
                copyOut { LinuxRoot / path }
                paths.forEach { copyOut { _ -> LinuxRoot / it } }
            }
            hostPath(LinuxRoot / path)
        }

    /**
     * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
     */
    fun guestfish(
        logger: RenderingLogger? = BACKGROUND,
        trace: Boolean = false,
        caption: (List<GuestfishCommand>.() -> String)? = null,
        bordered: Boolean = false,
        init: Init<GuestfishCommandsContext>,
    ): DockerExec = GuestfishCommandLine.build(this) {
        options {
            readWrite by true
            disk { it.file }
            this.trace by trace
            verbose by false
        }
        commands(init)
    }.run {
        exec.logging(logger ?: MutedRenderingLogger, exchangeDirectory) {
            block {
                caption { caption?.let { commands.it() } ?: "Running ${summary} …" }
                border = if (bordered) SOLID else DOTTED
                decorationFormatter { Formatter { it.ansi.blue.done } }
            }
        }
    }

    /**
     * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
     */
    fun virtCustomize(
        logger: RenderingLogger? = BACKGROUND,
        trace: Boolean = false,
        init: Init<CustomizationsContext>,
    ): DockerExec = VirtCustomizeCommandLine.build(this) {
        options {
            disk { it.file }
            if (!isDebugging) colors { on }
            if (trace) trace { on }
        }
        customizations(init)
    }.exec.logging(logger ?: MutedRenderingLogger, exchangeDirectory) {
        block {
            caption { "Running ${it.summary} …" }
            decorationFormatter { Formatter { it.ansi.brightBlue.done } }
        }
    }

    fun increaseDiskSpace(size: Size, logger: FixedWidthRenderingLogger? = BACKGROUND): Any =
        (logger ?: MutedRenderingLogger).logging("Increasing disk space: ${path.getSize()} ➜ ${size.formattedAs.input}", null) {
            var missing = size - path.getSize()
            val bytesPerStep = 200.Mega.bytes
            val oneHundredMegaBytes = ByteArray(bytesPerStep.wholeBytes.toInt()) { 0 }
            when {
                missing < 0.bytes -> {
                    logStatus { IO.Error typed "Requested disk space is ${-missing} smaller than current size of ${path.fileName} (${path.getSize()})." }
                    logStatus { IO.Error typed "Decreasing an image's disk space is currently not supported." }
                }
                missing == 0.bytes -> {
                    logStatus { IO.Output typed "${path.fileName} is has already ${path.getSize()}" }
                }
                else -> {
                    compactLogging("Progress:") {
                        logStatus { IO.Output typed path.getSize().toString() }
                        while (missing > 0.bytes) {
                            val write = if (missing < bytesPerStep) ByteArray(missing.wholeBytes.toInt()) { 0 } else oneHundredMegaBytes
                            path.appendBytes(write)
                            missing -= write.size
                            logStatus { IO.Output typed "⥅ ${path.getSize()}" }
                        }
                        path.getSize()
                    }

                    logLine { "Image Disk Space Successfully increased to " + path.getSize().toString() + "." }
                }
            }
        }

    override fun toString(): String = fullName

    companion object {
        infix fun OperatingSystem.based(path: Path): OperatingSystemImage = OperatingSystemImage(
            operatingSystem = this,
            path = path
        )

        private const val EXCHANGE_DIRECTORY_NAME = "shared"

        /**
         * Returns the directory used to share (i.e. for copying-in and -out)
         * files between this host and the [OperatingSystem] contained in the
         * given [disk] file.
         *
         * Example: For [disk] `$HOME/.imgcstmzr/project/os.img` the mount root
         * would be `$HOME/.imgcstmzr/project/shared`.
         */
        fun mountRootForDisk(disk: Path): Path =
            disk.resolveSibling(EXCHANGE_DIRECTORY_NAME).createDirectories()
    }
}
