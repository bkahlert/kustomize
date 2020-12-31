package com.imgcstmzr.patch

import koodies.text.withRandomSuffix

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
