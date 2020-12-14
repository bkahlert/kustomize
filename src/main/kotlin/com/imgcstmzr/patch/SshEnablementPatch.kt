package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.runtime.OperatingSystem

/**
 * Activates SSH.
 */
class SshEnablementPatch(os: OperatingSystem) :
    Patch by buildPatch(os, "Enable SSH", {
        guestfish {
            touch { it.resolveOnDisk("/boot/ssh") }
        }
    })
