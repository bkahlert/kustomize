package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

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
        virtCustomize {
            copyIn(LinuxRoot.etc.wpa_supplicant.wpa_supplicant_conf, fileContent)
        }
    }
}
