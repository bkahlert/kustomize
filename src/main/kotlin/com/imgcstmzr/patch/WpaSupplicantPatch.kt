package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import koodies.text.LineSeparators.lines

class WpaSupplicantPatch(
    private val fileContent: String,
) : Patch by buildPatch("WPA supplicant (${fileContent.lines().size} lines)", {
    customizeDisk {
        copyIn(WPA_SUPPLICANT, fileContent)
    }
}) {
    companion object {
        val WPA_SUPPLICANT: DiskPath = DiskPath("/etc/wpa_supplicant/wpa_supplicant.conf")
    }
}
