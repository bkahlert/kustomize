package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.lines
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * appends text to files (specified by the [contentToDiskMappings]'s contents)
 * into the disk images under each [contentToDiskMappings]'s [DiskPath].
 */
class AppendToFilesPatch(private val contentToDiskMappings: Map<String, DiskPath>) :
    Patch by buildPatch("Append Lines: " + contentToDiskMappings.map { (content, to) -> "${content.lines().size} line(s) ➜ ${to.fileName}" }.joinToString(", "),
        {

            // no obvious way to pass non-trivial text like HTML tags or even binary data to virt-customize
            // also fails to manipulate any files at /boot ...
//        customizeDisk {
//            contentToDiskMappings.forEach { (content, diskPath) ->
//                content.lines().forEach { line ->
//                    appendLine { line to diskPath }
//                }
//            }
//        }

            // ... therefore files are manipulated on the host
            files {
                contentToDiskMappings.forEach { (content, path) ->
                    edit(path, { require(it.readText().trim().endsWith(content.trim())) }) {
                        if (!it.exists()) {
                            it.writeText(content)
                        } else {
                            if (!it.readText().hasTrailingLineSeparator) it.appendText(LF)
                            it.appendText(content)
                        }
                        if (!content.hasTrailingLineSeparator) it.appendText(LF)
                    }
                }
            }

        }) {
    constructor(vararg contentToDiskMappings: Pair<String, DiskPath>) : this(contentToDiskMappings.toMap())
}
