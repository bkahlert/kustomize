package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage
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
    private val shellScripts: List<ShellScript>,
) : (OperatingSystemImage) -> PhasedPatch {

    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())
    constructor(name: String? = null, init: ScriptInit) : this(ShellScript(name, init))

    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Add ${shellScripts.size} First Boot Script(s): ${shellScripts.mapNotNull { it.name }.map { LineSeparators.LF + it }.joinToString("")}",
        osImage,
    ) {
        if (shellScripts.isNotEmpty()) {
            customizeDisk {
                shellScripts.forEach { firstBoot(it) }
            }
        }
    }
}
