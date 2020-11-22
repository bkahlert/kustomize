package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.nio.file.exists
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
import java.nio.file.Path

/**
 * An [image] holding an [operatingSystem] that can be accessed using the specified [credentials].
 */
class OperatingSystemImage(
    val operatingSystem: OperatingSystem,
    /**
     * The credentials that give access to the [OperatingSystem].
     */
    var credentials: Credentials = operatingSystem.defaultCredentials,
    private val image: Path,
) : Path by image, OperatingSystem by operatingSystem {
    companion object {
        infix fun OperatingSystem.based(image: Path): OperatingSystemImage = OperatingSystemImage(
            operatingSystem = this,
            image = image
        )
    }

    override val fullName: String get() = "${operatingSystem.fullName}${if (updatedCredentials) "*".magenta() else ""} at ${image.toUri()}"
    val shortName: String get() = "${operatingSystem.fullName}${if (updatedCredentials) "*".magenta() else ""} at ${image.fileName}"
    val path: String get() = "$image"

    private val updatedCredentials: Boolean get() = credentials != defaultCredentials

    fun boot(logger: RenderingLogger<Any>, vararg programs: Program): Int =
        ArmRunner.run(name = serialized, osImage = this, logger = logger, programs = programs)

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
                    singleLineLogger<Size>("Progress:") {
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

                    @Suppress("SpellCheckingInspection")
                    val compileScript = OperatingSystems.RaspberryPiLite.compileScript("expand-root", "sudo raspi-config --expand-rootfs")
                    boot(logger = this, programs = arrayOf(compileScript))
                }
            }
        }

    val exists: Boolean get() = image.exists

    override fun toString(): String = fullName
}
