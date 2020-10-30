package com.imgcstmzr.runtime

import com.imgcstmzr.util.quoted
import java.nio.file.Path

class OperatingSystemImage(private val operatingSystem: OperatingSystem, private val image: Path) : Path by image, OperatingSystem by operatingSystem {
    companion object {
        infix fun OperatingSystem.based(image: Path): OperatingSystemImage = OperatingSystemImage(this, image)
    }

    override fun toString(): String = "${operatingSystem.name} ⃕ ${image.quoted}"
}
