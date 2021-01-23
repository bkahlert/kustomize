package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.shell.ShellScript
import koodies.shell.ShellScript.Companion.build
import koodies.text.LineSeparators.LF

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * runs a specified [ShellScript] instances one-by-one and
 * when done shuts down.
 */
class ShellScriptPatch(
    shellScripts: List<ShellScript>,
) : Patch by buildPatch("${shellScripts.size} shell script(s):${shellScripts.mapNotNull { it.name }.map { LF + it }.joinToString("")}", {
    customizeDisk {
        firstBoot("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞o Setup") { osImage ->
            shellScripts.forEach { embed(it) }
            !osImage.shutdownCommand
        }
    }

    boot()
}) {
    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())
    constructor(name: String? = null, init: ShellScript.() -> Unit) : this(init.build(name))
}
