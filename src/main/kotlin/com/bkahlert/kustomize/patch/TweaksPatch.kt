package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * allows to set the number of attempts apt calls should try
 * to accomplish its goal.
 */
class TweaksPatch(
    val aptRetries: Int,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Tweaks (APT Retries)",
        osImage
    ) {

        guestfish {
            writeAppendLine(LinuxRoot.etc.apt.apt_conf_d.`80_retries`, """APT::Acquire::Retries "$aptRetries";""")
        }

    }
}
