package com.imgcstmzr.os

import com.imgcstmzr.cli.Layouts
import com.imgcstmzr.libguestfs.GuestfishCommandLine
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder.GuestfishCommandsContext
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.imgcstmzr.os.OperatingSystem.Credentials
import koodies.Exceptions.IAE
import koodies.builder.Init
import koodies.docker.DockerExec
import koodies.io.path.duplicate
import koodies.io.path.getSize
import koodies.io.path.pathString
import koodies.runtime.isDebugging
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs
import koodies.tracing.rendering.spanningLine
import koodies.tracing.spanning
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

    fun copyOut(path: String, vararg paths: String, trace: Boolean = false): Path =
        spanning("Copying out ${path.formattedAs.input}" + if (paths.isNotEmpty()) " and ${paths.size} other files" else "") {
            guestfish(trace) {
                copyOut { LinuxRoot / path }
                paths.forEach { copyOut { _ -> LinuxRoot / it } }
            }
            hostPath(LinuxRoot / path)
        }

    /**
     * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
     */
    fun guestfish(
        trace: Boolean = false,
        init: Init<GuestfishCommandsContext>,
    ): DockerExec = GuestfishCommandLine(this) {
        options {
            readWrite by true
            disk { it.file }
            this.trace by trace
            verbose by false
        }
        commands(init)
    }.exec.logging(exchangeDirectory, decorationFormatter = { it.toString().ansi.green.done }, layout = Layouts.DESCRIPTION)

    /**
     * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
     */
    fun virtCustomize(
        trace: Boolean = false,
        init: Init<CustomizationsContext>,
    ): DockerExec = VirtCustomizeCommandLine.build(this) {
        options {
            disk { it.file }
            if (!isDebugging) colors { on }
            if (trace) trace { on }
        }
        customizations(init)
    }.exec.logging(exchangeDirectory, decorationFormatter = { it.toString().ansi.green.done }, layout = Layouts.DESCRIPTION)

    fun increaseDiskSpace(size: Size): Unit =
        spanningLine("Increasing disk space: ${path.getSize()} ➜ ${size.formattedAs.input}") {
            var missing = size - path.getSize()
            require(missing >= Size.ZERO) {
                throw IAE(
                    "Requested disk space is ${-missing} smaller than current size of ${path.fileName} (${path.getSize()}).",
                    "Decreasing an image's disk space is currently not supported.",
                )
            }

            if (missing == Size.ZERO) return@spanningLine

            val bytesPerStep = 200.Mega.bytes
            val oneHundredMegaBytes = ByteArray(bytesPerStep.wholeBytes.toInt())
            log(path.getSize().toString())
            while (missing > 0.bytes) {
                val write = if (missing < bytesPerStep) ByteArray(missing.wholeBytes.toInt()) { 0 } else oneHundredMegaBytes
                path.appendBytes(write)
                missing -= write.size
                log(path.getSize().toString())
            }
            path.getSize()
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
