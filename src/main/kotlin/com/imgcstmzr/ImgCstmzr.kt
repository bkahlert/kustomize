package com.imgcstmzr

import koodies.io.selfCleaning
import java.nio.file.Path

object ImgCstmzr : koodies.io.Locations {

    override val Temp: Path by super.Temp.resolve("imgcstmzr").selfCleaning()

    /**
     * Directory in which built images and needed resources are stored.
     */
    val Cache: Path by lazy { ImgCstmzr.HomeDirectory.resolve(".imgcstmzr") }
}
