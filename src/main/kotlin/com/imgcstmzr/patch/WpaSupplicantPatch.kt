package com.imgcstmzr.patch

import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import koodies.text.LineSeparators.lines

class WpaSupplicantPatch(
    private val fileContent: String,
) : Patch by buildPatch("Inject WPA Supplicant (${fileContent.lines().size} lines)", {
    customizeDisk {
        copyIn(WPA_SUPPLICANT, fileContent)
    }
}) {
    companion object {
        val WPA_SUPPLICANT: DiskPath = LinuxRoot.etc / "wpa_supplicant" / "wpa_supplicant.conf"
    }
}
