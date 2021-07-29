package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage

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
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Add ${authorizedKeys.size} SSH Key(s) for $username",
        osImage,
    ) {
        customizeDisk {
            authorizedKeys.forEach { password -> sshInject { username to password } }
        }
    }
}
