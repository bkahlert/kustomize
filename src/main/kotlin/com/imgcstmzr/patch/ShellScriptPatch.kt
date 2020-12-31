package com.imgcstmzr.patch

import koodies.shell.ShellScript
import koodies.shell.ShellScript.Companion.build

class ShellScriptPatch(
    shellScripts: List<ShellScript>,
) : Patch by buildPatch("${shellScripts.size} shell script(s) with ${shellScripts.sumBy { it.lines.size }} lines altogether", {
    customizeDisk {
        firstBoot("Fist Boot") { osImage ->
            shellScripts.forEach { embed(it) }
            !osImage.shutdownCommand
        }
    }

    boot()
}) {
    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())
    constructor(name: String? = null, init: ShellScript.() -> Unit) : this(init.build(name))
}
