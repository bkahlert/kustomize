package com.imgcstmzr

import koodies.io.autoCleaning
import java.nio.file.Path

object Locations : koodies.io.Locations {
    override val Temp: Path by super.Temp.autoCleaning("com.imgcstmzr")
}
