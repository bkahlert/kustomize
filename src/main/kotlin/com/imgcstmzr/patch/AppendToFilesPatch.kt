package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.text.LineSeparators.lines

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * appends text to files (specified by the [contentToDiskMappings]'s contents)
 * into the disk images under each [contentToDiskMappings]'s [DiskPath].
 */
class AppendToFilesPatch(private val contentToDiskMappings: Map<String, DiskPath>) :
    Patch by buildPatch(contentToDiskMappings.map { (content, to) -> "${content.lines().size} line(s) ➜ ${to.fileName}" }.joinToString(", "), {

        customizeDisk {
            contentToDiskMappings.forEach { (content, diskPath) ->
                content.lines().forEach { line ->
                    appendLine { line to diskPath }
                }
            }
        }

    }) {
    constructor(vararg contentToDiskMappings: Pair<String, DiskPath>) : this(contentToDiskMappings.toMap())
}
