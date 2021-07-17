package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystem
import com.imgcstmzr.os.OperatingSystemImage
import koodies.text.withRandomSuffix

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set the host name of the [OperatingSystem] to [hostname]
 * optionally individualized with a [randomSuffix].
 */
class HostnamePatch(
    private val hostname: String,
    private val randomSuffix: Boolean,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Set Hostname to $hostname",
        osImage,
    ) {
        customizeDisk {
            hostname {
                if (randomSuffix) hostname.withRandomSuffix()
                else hostname
            }
        }
    }
}
