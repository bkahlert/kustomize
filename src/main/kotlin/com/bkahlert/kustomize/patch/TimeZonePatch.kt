package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage
import java.util.TimeZone

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * configures the timezone of the patched host.
 */
class TimeZonePatch(private val timeZone: TimeZone) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Set Time Zone to ${timeZone.displayName}",
        osImage,
    ) {
        customizeDisk { timeZone { timeZone } }
    }
}
