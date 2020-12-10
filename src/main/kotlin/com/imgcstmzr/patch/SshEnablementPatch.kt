package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.toPath

/**
 * Activates SSH.
 */
class SshEnablementPatch : Patch by buildPatch("Enable SSH", {
    customize {
        touch { "/boot/ssh".toPath() }
    }
})
