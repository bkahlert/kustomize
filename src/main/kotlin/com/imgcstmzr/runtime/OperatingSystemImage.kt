package com.imgcstmzr.runtime

import com.bkahlert.koodies.nio.file.conditioned
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.imgcstmzr.runtime.OperatingSystems.Companion.Credentials
import com.imgcstmzr.runtime.log.RenderingLogger
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

    val defaultCredentials: Credentials = Credentials(operatingSystem.defaultUsername, operatingSystem.defaultPassword)

    val updatedCredentials: Boolean get() = credentials != defaultCredentials

    fun boot(logger: RenderingLogger<Any>, vararg programs: Program): Int =
        ArmRunner.run(name = conditioned, osImage = this, logger = logger, programs = programs)

    override fun toString(): String = "${operatingSystem.name}${if (updatedCredentials) "*".magenta() else ""} at ${image.toUri()}"
}
