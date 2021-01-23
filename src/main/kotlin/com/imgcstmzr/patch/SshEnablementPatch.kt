package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.runtime.OperatingSystemImage

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
            touch { DiskPath("/boot/ssh") }
        }
    })
