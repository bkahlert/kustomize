package com.imgcstmzr.patch

import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.ShellScript.Companion.build

class ShellScriptPatch(
    shellScripts: List<ShellScript>,
) : Patch by buildPatch("${shellScripts.size} shell script(s) with ${shellScripts.sumBy { it.lines.size }} lines altogether", {
    customizeDisk {
        shellScripts.forEach {
            firstBoot(it)
        }
    }

    os {
        script("firstboot scripts", "echo 'Done'")
    }
}) {
    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())
    constructor(init: ShellScript.() -> Unit) : this(init.build())
}
