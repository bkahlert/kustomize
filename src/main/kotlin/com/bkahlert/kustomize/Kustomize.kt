package com.bkahlert.kustomize

import com.bkahlert.kommons.SemVer
import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.selfCleaning
import com.bkahlert.kommons.io.useRequiredClassPath
import com.bkahlert.kommons.time.hours
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream

object Kustomize : Locations {

    override val temp: Path by super.temp.resolve("kustomize").selfCleaning()

    // HACK download to kommons sub directory so that Docker users only
    // have to bind mount the kommons directory.
    // To bind mount in general is necessary because the host's Docker agent
    // is used and that agent needs to have access to shared space.
    // TODO make configurable
    val download by super.temp.resolve("kommons/download").selfCleaning(1.hours, 5)

    private val buildProperties: Properties by lazy {
        useRequiredClassPath("build.properties") { Properties().apply { load(it.inputStream()) } }
    }

    /**
     * Name of this library.
     */
    val name: String
        get() = buildProperties["name"]?.toString() ?: error("Cannot find name in build properties")

    /**
     * Group name of this library.
     */
    val group: String
        get() = buildProperties["group"]?.toString() ?: error("Cannot find group in build properties")

    /**
     * Version of this library.
     */
    val version: SemVer
        get() = SemVer.parse(buildProperties["version"]?.toString() ?: error("Cannot find version in build properties"))
}
