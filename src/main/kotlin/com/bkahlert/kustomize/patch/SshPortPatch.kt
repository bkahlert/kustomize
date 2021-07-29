package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * sets the SSH port to [port].
 *
 * @see SshEnablementPatch
 * @see SshAuthorizationPatch
 */
class SshPortPatch(
    private val port: Int,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build("Set SSH port to $port", osImage) {
        customizeDisk {
            firstBootCommand { "sed -i 's/^\\#Port 22\$/Port $port/g' /etc/ssh/sshd_config" }
        }
    }
}
