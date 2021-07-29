package com.bkahlert.kustomize

import koodies.io.Locations
import koodies.io.SelfCleaningDirectory.CleanUpMode.OnStart
import koodies.io.path.asPath
import koodies.io.selfCleaning
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object TestKustomize : Locations {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by Locations.Temp.resolve("kustomize-test").selfCleaning(Duration.ZERO, 0, cleanUpMode = OnStart)

    /**
     * Directory in which built images and needed resources are stored.
     */
    val TestCacheDirectory: Path = ".cache".asPath()
}
