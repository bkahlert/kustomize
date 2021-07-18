package com.imgcstmzr.patch

import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage

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

        customizeDisk {
            appendLine { """APT::Acquire::Retries "$aptRetries";""" to LinuxRoot.etc.apt.apt_conf_d.`80_retries` }
        }

    }
}
