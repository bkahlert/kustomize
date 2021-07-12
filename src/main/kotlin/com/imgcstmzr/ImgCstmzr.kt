package com.imgcstmzr

import koodies.io.selfCleaning
import java.nio.file.Path

object ImgCstmzr : koodies.io.Locations {
    override val Temp: Path by super.Temp.selfCleaning("com.imgcstmzr")

    /**
     * Directory in which built images and needed resources are stored.
     */
    val Cache: Path by lazy { ImgCstmzr.HomeDirectory.resolve(".imgcstmzr") }
}
