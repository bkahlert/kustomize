package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * disables the power-safe mode for the wifi adapter.
 */
class WifiPowerSafeModePatch : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Disable Wifi Power-Safe Mode",
        osImage
    ) {
        virtCustomize {
            firstBoot("Disable Wifi Power-Safe Mode") {
                file(LinuxRoot.etc.rc_local.pathString) {
                    removeLine("echo 0")
                    appendLine("/sbin/iw dev wlan0 set power_save off")
                    appendLine("echo 0")
                }
            }
        }
    }
}
