package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * sets the SSH port to [port].
 *
 * @see SshEnablementPatch
 * @see SshAuthorizationPatch
 */
class SshPortPatch(
    private val port: Int,
) : PhasedPatch by PhasedPatch.build("Set SSH port to $port", {

    customizeDisk {
        firstBootCommand { "sed -i 's/^\\#Port 22\$/Port $port/g' /etc/ssh/sshd_config" }
    }
})
