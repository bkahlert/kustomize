package com.imgcstmzr.patch

import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.text.withRandomSuffix

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set the host name of the [OperatingSystem] to [hostname]
 * optionally individualized with a [randomSuffix].
 */
class HostnamePatch(
    private val hostname: String,
    private val randomSuffix: Boolean,
) : Patch by buildPatch("hostname change to $hostname", {
    customizeDisk {
        hostname {
            hostname.let {
                if (randomSuffix) it.withRandomSuffix()
                else it
            }
        }
    }
})
