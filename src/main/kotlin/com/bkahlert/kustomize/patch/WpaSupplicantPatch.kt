package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.LinuxRoot.etc.wpa_supplicant.wpa_supplicant_conf
import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.text.LineSeparators.lines

/**
 * Sets the `wpa_supplicant.conf` to the specified [fileContent].
 */
class WpaSupplicantPatch(
    private val fileContent: String,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Inject WPA Supplicant (${fileContent.lines().size} lines)",
        osImage,
    ) {
        customizeDisk {
            copyIn(wpa_supplicant_conf, fileContent)
        }
    }
}
