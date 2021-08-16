package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.text.LineSeparators.LF

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * runs all specified [ShellScript] instances.
 */
class ShellScriptPatch(
    private val shellScripts: List<ShellScript>,
) : (OperatingSystemImage) -> PhasedPatch {

    constructor(vararg shellScripts: ShellScript) : this(shellScripts.toList())

    override fun invoke(osImage: OperatingSystemImage): PhasedPatch =
        PhasedPatch.build(
            "${shellScripts.size} Shell Script(s):${shellScripts.mapNotNull { it.name }.joinToString("") { LF + it }}",
            osImage,
        ) {

            virtCustomize {
                shellScripts.forEach { firstBoot(it) }
            }

            bootOs = true
        }
}
