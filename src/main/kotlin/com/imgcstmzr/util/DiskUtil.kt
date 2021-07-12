package com.imgcstmzr.util

import com.imgcstmzr.util.Disk.Companion.flashDiskOf
import koodies.exec.output
import koodies.shell.ShellScript
import koodies.tracing.rendering.spanningLine
import koodies.unit.bytes

object DiskUtil {
    @Suppress("SpellCheckingInspection")
    const val command: String = "diskutil"

    private fun listOutput(): String =
        ShellScript { command(command, "list", "-plist", "external", "physical") }.exec.logging().io.output.ansiRemoved

    fun listDisks(): Set<Disk> =
        spanningLine("Listing physical external disks") { disks }

    val disks: Set<Disk>
        get() = XML.from(listOutput()).findNodes("//dict/key[text()='DeviceIdentifier']").mapNotNull { node ->
            val id = node.findSibling { nodeName == "string" }?.textContent
                ?: throw IllegalStateException("Could not parse disk identifier")
            val capacity = node.findSibling { nodeName == "integer" }?.textContent?.toBigDecimal()?.bytes
                ?: throw IllegalStateException("Could not parse capacity")
            if (DiskIdentifier.isDisk(id)) flashDiskOf(id, capacity) else null
        }.toSet()
}
