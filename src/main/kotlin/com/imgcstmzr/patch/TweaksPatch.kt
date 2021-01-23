package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.runtime.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * allows to set the number of attempts apt calls should try
 * to accomplish its goal.
 */
class TweaksPatch(
    val aptRetries: Int,
) : Patch by buildPatch("Tweaks (apt retries)", {

    customizeDisk {
        appendLine { """APT::Acquire::Retries "$aptRetries";""" to APT_CONF_RETRIES }
    }

}) {
    companion object {
        val APT_CONF_RETRIES: DiskPath = DiskPath("/etc/apt/apt.conf.d/80-retries")
    }
}
