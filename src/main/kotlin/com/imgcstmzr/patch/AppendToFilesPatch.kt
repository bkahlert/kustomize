package com.imgcstmzr.patch

import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.withTrailingLineSeparator
import koodies.text.joinLinesToString
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
                    edit(path, {
                        val actualLines = it.readText().lines().flatMap { line -> line.lines() }
                        val expectedLines = content.withTrailingLineSeparator().lines()
                        val actualLastLines = actualLines.takeLast(expectedLines.size)
                        require(actualLastLines == expectedLines) {
                            "Expected:$LF${expectedLines.joinLinesToString()}$LF${LF}Actual:$LF${actualLastLines.joinLinesToString()}"
                        }
                    }) {
                        if (!it.exists()) {
                            // "Also a newline is added to the end of the LINE string automatically."
                            // https://libguestfs.org/virt-customize.1.html#customization-options
                            it.writeText(content.withTrailingLineSeparator())
                        } else {
                            // "If the file does not already end with a newline, then one is added before the appended line."
                            // https://libguestfs.org/virt-customize.1.html#customization-options
                            val actual = it.readText()
                            val lineSeparator = LineSeparators.autoDetect(actual)
                            if (!actual.hasTrailingLineSeparator) it.writeText(lineSeparator)

                            // "Also a newline is added to the end of the LINE string automatically."
                            // https://libguestfs.org/virt-customize.1.html#customization-options
                            it.appendText(content.withTrailingLineSeparator(lineSeparator = lineSeparator))
                        }
                    }
                }
            }

        }) {

    constructor(vararg contentToDiskMappings: Pair<String, DiskPath>) : this(contentToDiskMappings.toMap())
}
