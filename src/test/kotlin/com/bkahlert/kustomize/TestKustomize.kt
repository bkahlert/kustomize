package com.bkahlert.kustomize

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.SelfCleaningDirectory.CleanUpMode.OnStart
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.selfCleaning
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object TestKustomize : Locations {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val testRoot: Path by Locations.temp.resolve("kustomize-test").selfCleaning(Duration.ZERO, 0, cleanUpMode = OnStart)

    /**
     * Directory in which built images and needed resources are stored.
     */
    val testCacheDirectory: Path = ".cache".asPath()
}
