package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import koodies.shell.ScriptInit
import koodies.shell.ShellScript
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
) : PhasedPatch by PhasedPatch.build("Add ${shellScripts.size} First Boot Script(s): ${
    shellScripts.mapNotNull { it.name }.map { LineSeparators.LF + it }.joinToString("")
}", {
    customizeDisk {
        firstBoot("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞o First Boot") {
            shellScripts.forEach { embed(it, true) }
            ""
        }
    }
}) {
    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())
    constructor(name: String? = null, init: ScriptInit) : this(ShellScript(name, init))
}
