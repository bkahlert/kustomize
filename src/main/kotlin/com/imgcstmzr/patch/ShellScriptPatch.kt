package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import koodies.shell.ScriptInit
import koodies.shell.ShellScript
import koodies.text.LineSeparators.LF

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * runs a specified [ShellScript] instances one-by-one and
 * when done shuts down.
 */
class ShellScriptPatch(
    private val shellScripts: List<ShellScript>,
) : (OperatingSystemImage) -> PhasedPatch {

    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())
    constructor(name: String? = null, init: ScriptInit) : this(ShellScript(name, init))

    override fun invoke(osImage: OperatingSystemImage): PhasedPatch =
        PhasedPatch.build(
            "${shellScripts.size} Shell Script(s):${shellScripts.mapNotNull { it.name }.map { LF + it }.joinToString("")}",
            osImage,
        ) {

            customizeDisk {
                firstBoot("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞o Setup") { osImage ->
                    shellScripts.forEach { embed(it, true) }
                    !osImage.shutdownCommand
                }
            }

            bootOs = true
        }
}
