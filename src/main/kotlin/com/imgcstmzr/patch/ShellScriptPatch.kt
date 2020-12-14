package com.imgcstmzr.patch

import com.bkahlert.koodies.shell.ShellScript
import com.imgcstmzr.runtime.OperatingSystem

class ShellScriptPatch(
    os: OperatingSystem,
    vararg shellScripts: ShellScript,
) : Patch by buildPatch(os, "${shellScripts.size} shell script(s) with ${shellScripts.sumBy { it.lines.size }} lines altogether", {
    customize {
        shellScripts.forEach {
            firstBoot(it)
        }
    }
})
