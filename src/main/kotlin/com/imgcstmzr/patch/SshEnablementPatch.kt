package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.resolveOnDisk

/**
 * Activates SSH.
 */
class SshEnablementPatch :
    Patch by buildPatch("Enable SSH", {
        guestfish {
            touch { it.resolveOnDisk("/boot/ssh") }
        }
    })
