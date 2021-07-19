package com.imgcstmzr

import koodies.io.selfCleaning
import koodies.time.hours
import java.nio.file.Path

object ImgCstmzr : koodies.io.Locations {

    override val Temp: Path by super.Temp.resolve("imgcstmzr").selfCleaning()
    val Download by Temp.resolve("download").selfCleaning(1.hours, 5)

    /**
     * Directory in which built images and needed resources are stored.
     */
    val Cache: Path by lazy { ImgCstmzr.HomeDirectory.resolve(".imgcstmzr") }
}
