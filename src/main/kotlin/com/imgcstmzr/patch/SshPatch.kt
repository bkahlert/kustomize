package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.exists
import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.util.touch

class SshPatch : Patch by buildPatch("Enable SSH", {
    files {
        edit("/boot/ssh", { require(it.exists) { "$it is missing" } }, { path ->
            path.touch()
        })
    }
})
