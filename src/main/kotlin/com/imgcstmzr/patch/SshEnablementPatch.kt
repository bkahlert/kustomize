package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.exists
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.util.touch

/**
 * Activates SSH.
 */
class SshEnablementPatch : Patch by buildPatch("Enable SSH", {
    files {
        edit("/boot/ssh", { require(it.exists) { "$it is missing" } }, { path ->
            path.touch()
        })
    }
})
