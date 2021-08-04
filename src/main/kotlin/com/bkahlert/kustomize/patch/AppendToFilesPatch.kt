package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.text.LineSeparators.lines

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * appends text to files (specified by the [contentToDiskMappings]'s contents)
 * into the disk images under each [contentToDiskMappings]'s [DiskPath].
 */
class AppendToFilesPatch(
    private val contentToDiskMappings: Map<String, DiskPath>,
) : (OperatingSystemImage) -> PhasedPatch {

    constructor(vararg contentToDiskMappings: Pair<String, DiskPath>) : this(contentToDiskMappings.toMap())

    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Append Lines: " + contentToDiskMappings.map { (content, to) -> "${content.lines().size} line(s) ➜ ${to.fileName}" }.joinToString(", "),
        osImage
    ) {

        guestfish {
            contentToDiskMappings.forEach { (content, diskPath) ->
                writeAppendLine(diskPath, content)
            }
        }
    }
}
