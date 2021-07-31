package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage
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
            "${shellScripts.size} Shell Script(s):${shellScripts.mapNotNull { it.name }.joinToString("") { LF + it }}",
            osImage,
        ) {

            virtCustomize {
                shellScripts.forEach { firstBoot(it) }
                firstBootShutdownCommand()
            }

            bootOs = true
        }
}
