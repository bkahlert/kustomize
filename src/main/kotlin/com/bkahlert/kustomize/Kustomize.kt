package com.bkahlert.kustomize

import koodies.io.selfCleaning
import koodies.time.hours
import java.nio.file.Path

object Kustomize : koodies.io.Locations {

    override val Temp: Path by super.Temp.resolve("kustomize").selfCleaning()

    // HACK download to koodies sub directory so that Docker users only
    // have to bind mount the koodies directory.
    // To bind mount in general is necessary because the host's Docker agent
    // is used and that agent needs to have access to shared space.
    // TODO make configurable
    val Download by super.Temp.resolve("koodies/download").selfCleaning(1.hours, 5)
}
