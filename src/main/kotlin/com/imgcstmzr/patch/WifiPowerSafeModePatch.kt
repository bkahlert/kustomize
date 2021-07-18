package com.imgcstmzr.patch

import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * disables the power-safe mode for the wifi adapter.
 */
class WifiPowerSafeModePatch : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Disable Wifi Power-Safe Mode",
        osImage
    ) {
        customizeDisk {
            firstBoot("Disable Wifi Power-Safe Mode") {
                file(osImage.hostPath(LinuxRoot.etc.rc_local)) {
                    removeLine("echo 0")
                    appendLine("/sbin/iw dev wlan0 set power_save off")
                    appendLine("echo 0")
                }
            }
        }
        bootOs = true
    }
}
