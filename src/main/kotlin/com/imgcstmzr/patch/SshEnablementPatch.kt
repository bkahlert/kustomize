package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.toPath
import com.imgcstmzr.runtime.OperatingSystem

/**
 * Activates SSH.
 */
class SshEnablementPatch(os: OperatingSystem) :
    Patch by buildPatch(os, "Enable SSH", {
        guestfish {
            touch("/boot/ssh".toPath())
        }
    })
