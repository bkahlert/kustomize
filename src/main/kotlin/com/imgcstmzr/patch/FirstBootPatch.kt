package com.imgcstmzr.patch

import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.shell.ShellScript
import koodies.shell.ShellScript.Companion.build
import koodies.text.LineSeparators

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * schedules the specified [ShellScript] instances for execution on next boot
 * **but in contrast to [ShellScriptPatch] does not boot**.
 *
 * This allows for scripts intended to be executed in the presence of the user.
 */
class FirstBootPatch(
    shellScripts: List<ShellScript>,
) : Patch by buildPatch("Schedule ${shellScripts.size} shell script(s) for execution on next boot:${
    shellScripts.mapNotNull { it.name }.map { LineSeparators.LF + it }.joinToString("")
}", {
    customizeDisk {
        firstBoot("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞o First Boot") {
            shellScripts.forEach { embed(it) }
        }
    }
}) {
    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())
    constructor(name: String? = null, init: ShellScript.() -> Unit) : this(init.build(name))
}
