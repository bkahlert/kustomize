package com.imgcstmzr.patch

import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * allows to set the number of attempts apt calls should try
 * to accomplish its goal.
 */
class TweaksPatch(
    val aptRetries: Int,
) : PhasedPatch by PhasedPatch.build("Tweaks (APT Retries)", {

    customizeDisk {
        appendLine { """APT::Acquire::Retries "$aptRetries";""" to APT_CONF_RETRIES }
    }

}) {
    companion object {
        val APT_CONF_RETRIES: DiskPath = LinuxRoot.etc / "apt" / "apt.conf.d" / "80-retries"
    }
}
