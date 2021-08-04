package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.shell.ShellScript

class BootAndShutdownPatch : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build("Boot and shutdown", osImage) {

        virtCustomize {
            firstBoot("Shutdown") { ShellScript { shutdown }.toString() }
        }

        bootOs = true
    }
}
