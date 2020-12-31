package com.imgcstmzr.patch

import koodies.io.path.withDirectoriesCreated
import koodies.text.LineSeparators.lines
import java.nio.file.Path
import kotlin.io.path.writeLines

class WpaSupplicantPatch(
    private val fileContent: String,
) : Patch by buildPatch("WPA supplicant (${fileContent.lines().size} lines)", {
    customizeDisk {
        copyIn(WPA_SUPPLICANT) {
            withDirectoriesCreated().writeLines(fileContent.lines())
        }
    }
}) {
    companion object {
        val WPA_SUPPLICANT: Path = Path.of("/etc/wpa_supplicant/wpa_supplicant.conf")
    }
}
