package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystemImage

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
) : Patch by buildPatch("Add ${authorizedKeys.size} authorized SSH keys to $username", {

    customizeDisk {
        authorizedKeys.forEach { password -> sshInject(username, password) }
    }
})
