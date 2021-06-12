package com.imgcstmzr.patch

import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.patch.Patch.Companion.buildPatch

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * activates SSH.
 *
 * @see SshAuthorizationPatch
 * @see SshPortPatch
 */
class SshEnablementPatch :
    Patch by buildPatch("Enable SSH", {
        guestfish {
            touch { LinuxRoot.boot / "ssh" }
        }
    })
