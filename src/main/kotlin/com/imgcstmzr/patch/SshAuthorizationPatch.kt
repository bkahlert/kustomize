package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.patch.Patch.Companion.buildPatch

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * add the specified [authorizedKeys] for user [username].
 *
 * This allows [username] to authenticate using one of the specified [authorizedKeys]
 * instead of a password.
 *
 * @see SshEnablementPatch
 * @see SshPortPatch
 */
class SshAuthorizationPatch(
    private val username: String,
    private val authorizedKeys: List<String>,
) : Patch by buildPatch("Add ${authorizedKeys.size} SSH Key(s) for $username", {

    customizeDisk {
        authorizedKeys.forEach { password -> sshInject { username to password } }
    }
})
