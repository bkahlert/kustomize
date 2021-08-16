package com.bkahlert.kustomize.util

import com.bkahlert.kommons.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.quoted
import com.bkahlert.kommons.tracing.rendering.spanningLine
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kommons.unit.BinaryPrefixes
import com.bkahlert.kommons.unit.DecimalPrefixes
import com.bkahlert.kommons.unit.Mega
import com.bkahlert.kommons.unit.Size
import com.bkahlert.kommons.unit.bytes
import com.bkahlert.kustomize.util.DiskUtil.disks
import com.bkahlert.kustomize.util.DiskUtil.listDisks
import java.nio.file.Path

class Disk private constructor(val id: DiskIdentifier, val capacity: Size) {

    val roundedCapacity get() = capacity.toString(DecimalPrefixes, decimals = 0)

    fun mountDisk(): Disk = spanningLine("Mounting ${this@Disk}") {
        ShellScript { command(DiskUtil.command, "mountDisk", id.name) }
        this@Disk
    }

    fun unmountDisk(): Disk = spanningLine("Un-mounting ${this@Disk}") {
        ShellScript { command(DiskUtil.command, "unmountDisk", id.name) }
        this@Disk
    }

    fun eject(): Disk = spanningLine("Ejecting ${this@Disk}") {
        ShellScript { command(DiskUtil.command, "eject", id.name) }
        this@Disk
    }

    fun flash(file: Path, megaBytesPerTime: Int = 32): Disk =
        runSpanning("Flashing to ${this@Disk} with ${megaBytesPerTime.Mega.bytes}/t") {
            unmountDisk()

            val command = "sudo dd if=${file.pathString.quoted} of=${id.device.quoted} bs=${megaBytesPerTime}m"
            log(command)
            val flashingProcess = ShellScript { command }.exec.logging(file.parent)

            if (flashingProcess.waitFor() is Succeeded) {
                "Success".ansi.green
                eject()
            } else {
                exception(RuntimeException("An error occurred running $command"))
                this@Disk
            }
        }

    fun Size.format() = toString(BinaryPrefixes)

    override fun toString(): String = "$id ($roundedCapacity)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Disk

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun flashDiskOf(id: String, capacity: Size): Disk {
            require(DiskIdentifier.isDisk(id)) { "$id is no valid disk identifier" }
            return Disk(DiskIdentifier(id), capacity)
        }

        fun flashDiskOf(id: String): Disk? =
            disks.find { it.id.name == id } ?: throw IllegalArgumentException("$id does not exist.")
    }
}

/**
 * Flashes a disk using [file]. If specified [disk] (e.g. `disk2`) is used.
 * Otherwise any available physical removable disk is used iff there is exactly one candidate.
 */
fun flash(file: Path, disk: String?): Disk? =
    runSpanning("Flashing ${file.fileName} (${file.getSize()})") {

        val disks = listDisks()

        val flashDisk: Disk? = disk?.let {
            disks.singleOrNull { it.id.name == disk }
        } ?: disks.singleOrNull()

        flashDisk?.run { flash(file) } ?: run {
            log(
                if (disk == null) {
                    if (disks.isEmpty()) "No flash-able disks found.".ansi.bold
                    else "Found multiple candidates: ${disks.joinToString(", ")}. Please explicitly choose one."
                } else {
                    if (disks.isEmpty()) "No disk $disk found."
                    else "Disk $disk is not among the flash-able disks: ${disks.joinToString(", ")}. Please choose another one."
                })
        }
        null
    }


@JvmInline
value class DiskIdentifier(val name: String) {
    val device: String get() = "/dev/$name"
    override fun toString(): String = "disk \"$name\""

    companion object {
        private val diskIdentifierPattern = Regex("disk\\d+")
        fun isDisk(id: String) = diskIdentifierPattern.matches(id)
    }
}
