package com.imgcstmzr

import koodies.io.Locations
import koodies.io.SelfCleaningDirectory.CleanUpMode.OnStart
import koodies.io.selfCleaning
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object ImgCstmzrTest : Locations {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by Locations.Temp.selfCleaning("com.imgcstmzr-test", Duration.ZERO, 0, cleanUpMode = OnStart)

    /**
     * Directory in which built images and needed resources are stored.
     */
    val TestCache: Path = ImgCstmzr.HomeDirectory.resolve(".imgcstmzr.test")
}
