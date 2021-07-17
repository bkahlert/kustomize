package com.imgcstmzr.patch

import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import koodies.text.LineSeparators.lines

class WpaSupplicantPatch(
    private val fileContent: String,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Inject WPA Supplicant (${fileContent.lines().size} lines)",
        osImage,
    ) {
        customizeDisk {
            copyIn(WPA_SUPPLICANT, fileContent)
        }
    }

    companion object {
        val WPA_SUPPLICANT: DiskPath = LinuxRoot.etc / "wpa_supplicant" / "wpa_supplicant.conf"
    }
}
