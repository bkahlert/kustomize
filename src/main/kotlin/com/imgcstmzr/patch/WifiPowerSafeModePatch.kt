package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import java.nio.file.Path

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * disables the power-safe mode for the wifi adapter.
 */
class WifiPowerSafeModePatch : PhasedPatch by PhasedPatch.build(NAME, {

    customizeDisk {
        firstBoot(NAME) {
            file(RC_LOCAL) {
                removeLine("echo 0")
                appendLine("/sbin/iw dev wlan0 set power_save off")
                appendLine("echo 0")
            }
        }
    }

    bootOs { yes }

}) {
    companion object {
        val RC_LOCAL: Path = Path.of("/etc/rc.local")
        private val NAME = "Disable Wifi Power-Safe Mode"
    }
}
