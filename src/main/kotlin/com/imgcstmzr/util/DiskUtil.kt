package com.imgcstmzr.util

import com.imgcstmzr.util.Disk.Companion.flashDiskOf
import koodies.concurrent.process.IO
import koodies.concurrent.process.logged
import koodies.concurrent.script
import koodies.logging.RenderingLogger
import koodies.logging.compactLogging
import koodies.unit.bytes

object DiskUtil {
    @Suppress("SpellCheckingInspection")
    const val command = "diskutil"

    private fun listOutput(): String {
        val script = script { command(command, "list", "-plist", "external", "physical") }
        return script.logged(IO.Type.OUT).unformatted
    }

    fun RenderingLogger?.listDisks(): Set<Disk> =
        compactLogging("Listing physical external disks") { disks }

    val disks: Set<Disk>
        get() = XML.from(listOutput()).findNodes("//dict/key[text()='DeviceIdentifier']").mapNotNull { node ->
            val id = node.findSibling { nodeName == "string" }?.textContent
                ?: throw IllegalStateException("Could not parse disk identifier")
            val capacity = node.findSibling { nodeName == "integer" }?.textContent?.toBigDecimal()?.bytes
                ?: throw IllegalStateException("Could not parse capacity")
            if (DiskIdentifier.isDisk(id)) flashDiskOf(id, capacity) else null
        }.toSet()
}
