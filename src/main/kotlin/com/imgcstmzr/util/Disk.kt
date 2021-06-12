package com.imgcstmzr.util

import com.imgcstmzr.logError
import com.imgcstmzr.util.DiskUtil.disks
import com.imgcstmzr.util.DiskUtil.listDisks
import koodies.exec.Process.State.Exited.Succeeded
import koodies.io.path.getSize
import koodies.io.path.pathString
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.MutedRenderingLogger
import koodies.shell.ShellScript
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.quoted
import koodies.unit.BinaryPrefix
import koodies.unit.DecimalPrefix
import koodies.unit.Mega
import koodies.unit.Size
import koodies.unit.bytes
import java.nio.file.Path

class Disk private constructor(val id: DiskIdentifier, val capacity: Size) {

    val roundedCapacity get() = capacity.toString<DecimalPrefix>(decimals = 0)

    fun FixedWidthRenderingLogger.mountDisk(): Disk = compactLogging("Mounting ${this@Disk}") {
        ShellScript { command(DiskUtil.command, "mountDisk", id.name) }
        this@Disk
    }

    fun FixedWidthRenderingLogger.unmountDisk(): Disk = compactLogging("Un-mounting ${this@Disk}") {
        ShellScript { command(DiskUtil.command, "unmountDisk", id.name) }
        this@Disk
    }

    fun FixedWidthRenderingLogger.eject(): Disk = compactLogging("Ejecting ${this@Disk}") {
        ShellScript { command(DiskUtil.command, "eject", id.name) }
        this@Disk
    }

    fun FixedWidthRenderingLogger.flash(file: Path, megaBytesPerTime: Int = 32): Disk =
        logging("Flashing to ${this@Disk} with ${megaBytesPerTime.Mega.bytes}/t") {
            unmountDisk()

            val command = "sudo dd if=${file.pathString.quoted} of=${id.device.quoted} bs=${megaBytesPerTime}m"
            logLine { command }
            val flashingProcess = ShellScript { command }.exec.logging(this, file.parent)

            if (flashingProcess.waitFor() is Succeeded) {
                "Success".ansi.green
                eject()
            } else {
                logError("An error occurred running $command")
                this@Disk
            }
        }

    fun Size.format() = toString<BinaryPrefix>()

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
        fun FixedWidthRenderingLogger.flashDiskOf(id: String, capacity: Size): Disk {
            require(DiskIdentifier.isDisk(id)) { "$id is no valid disk identifier" }
            return Disk(DiskIdentifier(id), capacity)
        }

        fun FixedWidthRenderingLogger.flashDiskOf(id: String): Disk? =
            disks.find { it.id.name == id } ?: throw IllegalArgumentException("$id does not exist.")
    }
}

/**
 * Flashes a disk using [file]. If specified [disk] (e.g. `disk2`) is used.
 * Otherwise any available physical removable disk is used iff there is exactly one candidate.
 */
fun FixedWidthRenderingLogger.flash(file: Path, disk: String?): Disk? =
    logging("Flashing ${file.fileName} (${file.getSize()})") {

        val disks = MutedRenderingLogger.listDisks()

        val flashDisk: Disk? = disk?.let {
            disks.singleOrNull { it.id.name == disk }
        } ?: disks.singleOrNull()

        flashDisk?.run { flash(file) } ?: run {
            logLine {
                if (disk == null) {
                    if (disks.isEmpty()) "No flash-able disks found.".ansi.bold
                    else "Found multiple candidates: ${disks.joinToString(", ")}. Please explicitly choose one."
                } else {
                    if (disks.isEmpty()) "No disk $disk found."
                    else "Disk $disk is not among the flash-able disks: ${disks.joinToString(", ")}. Please choose another one."
                }
            }
            null
        }
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
