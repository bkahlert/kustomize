package com.imgcstmzr.util

import com.imgcstmzr.util.Disk.Companion.flashDiskOf
import koodies.exec.output
import koodies.logging.FixedWidthRenderingLogger
import koodies.shell.ShellScript
import koodies.unit.bytes

object DiskUtil {
    @Suppress("SpellCheckingInspection")
    const val command = "diskutil"

    private fun FixedWidthRenderingLogger.listOutput(): String =
        ShellScript { command(command, "list", "-plist", "external", "physical") }.exec.logging(this).io.output.ansiRemoved

    fun FixedWidthRenderingLogger.listDisks(): Set<Disk> =
        compactLogging("Listing physical external disks") { disks }

    val FixedWidthRenderingLogger.disks: Set<Disk>
        get() = XML.from(listOutput()).findNodes("//dict/key[text()='DeviceIdentifier']").mapNotNull { node ->
            val id = node.findSibling { nodeName == "string" }?.textContent
                ?: throw IllegalStateException("Could not parse disk identifier")
            val capacity = node.findSibling { nodeName == "integer" }?.textContent?.toBigDecimal()?.bytes
                ?: throw IllegalStateException("Could not parse capacity")
            if (DiskIdentifier.isDisk(id)) flashDiskOf(id, capacity) else null
        }.toSet()
}
