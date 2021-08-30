package com.bkahlert.kustomize.util

import com.bkahlert.kommons.exec.output
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.tracing.rendering.runSpanningLine
import com.bkahlert.kommons.unit.bytes
import com.bkahlert.kustomize.util.Disk.Companion.flashDiskOf

object DiskUtil {

    @Suppress("SpellCheckingInspection")
    const val command: String = "diskutil"

    private fun listOutput(): String =
        ShellScript { command(command, "list", "-plist", "external", "physical") }.exec.logging().io.output.ansiRemoved

    fun listDisks(): Set<Disk> =
        runSpanningLine("Listing physical external disks") { disks }

    val disks: Set<Disk>
        get() = XML.from(listOutput()).findNodes("//dict/key[text()='DeviceIdentifier']").mapNotNull { node ->
            val id = node.findSibling { nodeName == "string" }?.textContent
                ?: throw IllegalStateException("Could not parse disk identifier")
            val capacity = node.findSibling { nodeName == "integer" }?.textContent?.toBigDecimal()?.bytes
                ?: throw IllegalStateException("Could not parse capacity")
            if (DiskIdentifier.isDisk(id)) flashDiskOf(id, capacity) else null
        }.toSet()
}
