package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * activates SSH.
 *
 * @see SshAuthorizationPatch
 * @see SshPortPatch
 */
class SshEnablementPatch : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Enable SSH",
        osImage,
    ) {
        guestfish {
            touch { LinuxRoot.boot.ssh }
        }
    }
}
