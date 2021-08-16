package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystem
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kommons.text.withRandomSuffix

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
        virtCustomize {
            hostname {
                if (randomSuffix) hostname.withRandomSuffix()
                else hostname
            }
        }
    }
}
