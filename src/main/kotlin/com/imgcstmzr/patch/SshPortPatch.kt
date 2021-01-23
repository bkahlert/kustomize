package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * sets the SSH port to [port].
 *
 * @see SshEnablementPatch
 * @see SshAuthorizationPatch
 */
class SshPortPatch(
    private val port: Int,
) : Patch by buildPatch("Set SSH port to $port", {

    customizeDisk {
        firstBootCommand { "sed -i 's/^\\#Port 22\$/Port $port/g' /etc/ssh/sshd_config" }
    }
})
