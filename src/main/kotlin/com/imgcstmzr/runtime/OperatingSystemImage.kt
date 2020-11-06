package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.nio.file.conditioned
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.unit.Mega
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.runtime.OperatingSystems.Companion.Credentials
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.singleLineLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.subLogger
import java.nio.file.Path

/**
 * An [image] holding an [operatingSystem] that can be accessed using the specified [credentials].
 */
class OperatingSystemImage(
    val operatingSystem: OperatingSystem,
    /**
     * The credentials that give access to the [OperatingSystem].
     */
    var credentials: Credentials = Credentials(operatingSystem.defaultUsername, operatingSystem.defaultPassword),
    private val image: Path,
) : Path by image, OperatingSystem by operatingSystem {
    companion object {
        infix fun OperatingSystem.based(image: Path): OperatingSystemImage = OperatingSystemImage(
            operatingSystem = this,
            image = image
        )
    }

    val fullName: String get() = "${operatingSystem.name}${if (updatedCredentials) "*".magenta() else ""} at ${image.toUri()}"
    val shortName: String get() = "${operatingSystem.name}${if (updatedCredentials) "*".magenta() else ""} at ${image.fileName}"
    val path: String get() = "$image"

    val defaultCredentials: Credentials = Credentials(operatingSystem.defaultUsername, operatingSystem.defaultPassword)

    val updatedCredentials: Boolean get() = credentials != defaultCredentials

    fun boot(logger: RenderingLogger<Any>, vararg programs: Program): Int =
        ArmRunner.run(name = conditioned, osImage = this, logger = logger, programs = programs)

    fun increaseDiskSpace(logger: RenderingLogger<Any>, size: Size): Any =
        logger.subLogger("Increasing Disk Space: ${image.size} ➜ $size", null) {
            var missing = size - image.size
            val bytesPerStep = 100.Mega.bytes
            val oneHundredMegaBytes = bytesPerStep.toZeroFilledByteArray()
            when {
                missing < 0.bytes -> {
                    logStatus { IO.Type.ERR typed "Requested disk space is ${-missing} smaller than current size of ${image.fileName} (${image.size})." }
                    logStatus { IO.Type.ERR typed "Decreasing an image's disk space is currently not supported." }
                }
                missing == 0.bytes -> {
                    logStatus { IO.Type.OUT typed "${image.fileName} is has already ${image.size}" }
                }
                else -> {
                    singleLineLogger<Any, Size>("Progress:") {
                        logStatus { IO.Type.OUT typed image.size.toString() }
                        while (missing > 0.bytes) {
                            val write = if (missing < bytesPerStep) missing.toZeroFilledByteArray() else oneHundredMegaBytes
                            image.toFile().appendBytes(write)
                            missing -= write.size
                            logStatus { IO.Type.OUT typed "⥅ ${image.size}" }
                        }
                        image.size
                    }

                    logLine { "Image Disk Space Successfully increased to " + image.size.toString() + ". Booting OS to finalize..." }

                    val compileScript = OperatingSystems.RaspberryPiLite.compileScript("expand-root", "sudo raspi-config --expand-rootfs")
                    boot(this, compileScript)
                }
            }
        }

    override fun toString(): String = fullName
}
