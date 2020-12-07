package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.docker.DockerContainerName.Companion.toUniqueContainerName
import com.bkahlert.koodies.nio.file.appendBytes
import com.bkahlert.koodies.nio.file.baseName
import com.bkahlert.koodies.nio.file.duplicate
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.unit.Mega
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.singleLineLogger
import com.imgcstmzr.runtime.log.subLogger
import com.imgcstmzr.util.isReadable
import java.nio.file.Path
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
    val fileName: String = path.fileName.serialized
    val readable: Boolean get() = path.isReadable
    val directory: Path get() = path.parent
    val file: Path get() = path
    val size: Size get() = path.size
    fun duplicate(): OperatingSystemImage = operatingSystem based (path.duplicate())

    fun newLogFilePath(): Path {
        val count = directory.listDirectoryEntries("${file.baseName}.*.log").size
        val index = count.toString().padStart(2, '0')
        val name = "${file.baseName}.$index.log"
        return directory.resolve(name)
    }

    private val updatedCredentials: Boolean get() = credentials != defaultCredentials

    fun boot(logger: RenderingLogger<Any>, vararg programs: Program): Int =
        execute(logger = logger, programs = programs)

    fun increaseDiskSpace(logger: RenderingLogger<Any>, size: Size): Any =
        logger.subLogger("Increasing Disk Space: ${path.size} ➜ $size", null) {
            var missing = size - path.size
            val bytesPerStep = 100.Mega.bytes
            val oneHundredMegaBytes = bytesPerStep.toZeroFilledByteArray()
            when {
                missing < 0.bytes -> {
                    logStatus { IO.Type.ERR typed "Requested disk space is ${-missing} smaller than current size of ${path.fileName} (${path.size})." }
                    logStatus { IO.Type.ERR typed "Decreasing an image's disk space is currently not supported." }
                }
                missing == 0.bytes -> {
                    logStatus { IO.Type.OUT typed "${path.fileName} is has already ${path.size}" }
                }
                else -> {
                    singleLineLogger<Size>("Progress:") {
                        logStatus { IO.Type.OUT typed path.size.toString() }
                        while (missing > 0.bytes) {
                            val write = if (missing < bytesPerStep) missing.toZeroFilledByteArray() else oneHundredMegaBytes
                            path.appendBytes(write)
                            missing -= write.size
                            logStatus { IO.Type.OUT typed "⥅ ${path.size}" }
                        }
                        path.size
                    }

                    logLine { "Image Disk Space Successfully increased to " + path.size.toString() + ". Booting OS to finalize..." }

                    @Suppress("SpellCheckingInspection")
                    val compileScript = OperatingSystems.RaspberryPiLite.compileScript("expand-root", "sudo raspi-config --expand-rootfs")
                    execute(path.toUniqueContainerName().sanitized, this, true, compileScript)
                }
            }
        }

    override fun toString(): String = fullName
}
