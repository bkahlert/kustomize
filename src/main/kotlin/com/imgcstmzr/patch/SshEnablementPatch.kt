package com.imgcstmzr.patch

import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * activates SSH.
 *
 * @see SshAuthorizationPatch
 * @see SshPortPatch
 */
class SshEnablementPatch :
    PhasedPatch by PhasedPatch.build("Enable SSH", {
        modifyDisk {
            touch { LinuxRoot.boot / "ssh" }
        }
    })
