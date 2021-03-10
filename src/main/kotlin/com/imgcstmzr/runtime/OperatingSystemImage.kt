package com.imgcstmzr.runtime

import com.imgcstmzr.runtime.OperatingSystem.Credentials
import koodies.concurrent.process.IO
import koodies.io.path.asString
import koodies.io.path.baseName
import koodies.io.path.duplicate
import koodies.io.path.getSize
import koodies.logging.BlockRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.compactLogging
import koodies.logging.logging
import koodies.terminal.AnsiColors.magenta
import koodies.unit.Mega
import koodies.unit.Size
import koodies.unit.bytes
import java.nio.file.Path
import kotlin.io.path.appendBytes
import kotlin.io.path.isReadable
import kotlin.io.path.listDirectoryEntries

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
    companion object {
        infix fun OperatingSystem.based(path: Path): OperatingSystemImage = OperatingSystemImage(
            operatingSystem = this,
            path = path
        )
    }

    override val fullName: String get() = "${operatingSystem.fullName}${if (updatedCredentials) "*".magenta() else ""} at ${path.toUri()}"
    val shortName: String get() = "${operatingSystem.fullName}${if (updatedCredentials) "*".magenta() else ""} at ${path.fileName}"
    val fileName: String = path.fileName.asString()
    val readable: Boolean get() = path.isReadable()
    val directory: Path get() = path.parent
    val file: Path get() = path
    val size: Size get() = path.getSize()
    fun duplicate(): OperatingSystemImage = operatingSystem based (path.duplicate())

    fun newLogFilePath(): Path {
        val count = directory.listDirectoryEntries("${file.baseName}.*.log").size
        val index = count.toString().padStart(2, '0')
        val name = "${file.baseName}.$index.log"
        return directory.resolve(name)
    }

    private val updatedCredentials: Boolean get() = credentials != defaultCredentials

    fun boot(logger: BlockRenderingLogger, vararg programs: Program): Int =
        execute(logger = logger, programs = programs)

    fun increaseDiskSpace(logger: RenderingLogger, size: Size): Any =
        logger.logging("Increasing Disk Space: ${path.getSize()} ➜ $size", null) {
            var missing = size - path.getSize()
            val bytesPerStep = 200.Mega.bytes
            val oneHundredMegaBytes = ByteArray(bytesPerStep.bytes.intValue()) { 0 }
            when {
                missing < 0.bytes -> {
                    logStatus { IO.Type.ERR typed "Requested disk space is ${-missing} smaller than current size of ${path.fileName} (${path.getSize()})." }
                    logStatus { IO.Type.ERR typed "Decreasing an image's disk space is currently not supported." }
                }
                missing == 0.bytes -> {
                    logStatus { IO.Type.OUT typed "${path.fileName} is has already ${path.getSize()}" }
                }
                else -> {
                    compactLogging("Progress:") {
                        logStatus { IO.Type.OUT typed path.getSize().toString() }
                        while (missing > 0.bytes) {
                            val write = if (missing < bytesPerStep) ByteArray(missing.bytes.intValue()) { 0 } else oneHundredMegaBytes
                            path.appendBytes(write)
                            missing -= write.size
                            logStatus { IO.Type.OUT typed "⥅ ${path.getSize()}" }
                        }
                        path.getSize()
                    }

                    logLine { "Image Disk Space Successfully increased to " + path.getSize().toString() + "." }
                }
            }
        }

    override fun toString(): String = fullName
}
