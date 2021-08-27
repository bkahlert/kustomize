package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.text.withRandomSuffix
import com.bkahlert.kustomize.os.OperatingSystem
import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set the host name of the [OperatingSystem] to [name]
 * optionally individualized with a [randomSuffix].
 *
 * Optionally a [prettyName] (e.g. `John's Device`),
 * an [iconName] (default: `computer`) and a [chassis] (default: `embedded`).
 */
class HostnamePatch(
    private val name: String,
    private val randomSuffix: Boolean,
    private val prettyName: String? = null,
    private val iconName: String? = null,
    private val chassis: String? = null,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Set Hostname to $name",
        osImage,
    ) {
        virtCustomize {
            val name = if (randomSuffix) name.withRandomSuffix() else name
            val prettySuffix = name.drop(this@HostnamePatch.name.length).dropWhile { it == '-' }.takeIf { it.isNotEmpty() }?.let { " — $it" } ?: ""

            hostname { name }

            firstBoot("set hostname") { command("hostnamectl", "set-hostname", name) }
            if (prettyName != null) firstBoot("set pretty hostname") { command("hostnamectl", "set-hostname", "--pretty", prettyName + prettySuffix) }
            if (iconName != null) firstBoot("set icon name") { command("hostnamectl", "set-icon-name", iconName) }
            if (chassis != null) firstBoot("set chassis") { command("hostnamectl", "set-chassis", chassis) }
        }
    }
}
