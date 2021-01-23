package com.imgcstmzr.util

import com.imgcstmzr.util.DiskUtil.listDisks
import koodies.concurrent.process
import koodies.concurrent.process.process
import koodies.concurrent.script
import koodies.io.path.asString
import koodies.logging.RenderingLogger
import koodies.logging.compactLogging
import koodies.logging.logging
import koodies.terminal.AnsiColors.green
import koodies.terminal.AnsiColors.red
import koodies.terminal.AnsiFormats.bold
import koodies.text.quoted
import koodies.unit.*
import java.nio.file.Path

class Disk private constructor(val id: DiskIdentifier, val capacity: Size) {

    val roundedCapacity get() = capacity.toString<DecimalPrefix>(decimals = 0)

    fun RenderingLogger?.mountDisk(): Disk = compactLogging("Mounting ${this@Disk}") {
        script(expectedExitValue = null) {
            command(DiskUtil.command, "mountDisk", id.name)
        }
        this@Disk
    }

    fun RenderingLogger?.unmountDisk(): Disk = compactLogging("Un-mounting ${this@Disk}") {
        script(expectedExitValue = null) {
            command(DiskUtil.command, "unmountDisk", id.name)
        }
        this@Disk
    }

    fun RenderingLogger?.eject(): Disk = compactLogging("Ejecting ${this@Disk}") {
        script(expectedExitValue = null) {
            command(DiskUtil.command, "eject", id.name)
        }
        this@Disk
    }

    fun RenderingLogger?.flash(file: Path, megaBytesPerTime: Int = 32): Disk =
        logging("Flashing to ${this@Disk} with ${megaBytesPerTime.Mega.bytes}/t") {
            unmountDisk()

            val command = "sudo dd if=${file.asString().quoted} of=${id.device.quoted} bs=${megaBytesPerTime}m"
            logLine { command }
            val flashingProcess = file.parent.process("/bin/sh", "-c", command, expectedExitValue = null).process { logLine { it } }

            val exitCode = flashingProcess.also { println(it.ioLog.logged()) }.waitForTermination()
            if (exitCode == 0) {
                "Success".green()
                eject()
            } else {
                logLine { "An error occurred running $command".red() }
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
        fun flashDiskOf(id: String, capacity: Size): Disk {
            require(DiskIdentifier.isDisk(id)) { "$id is no valid disk identifier" }
            return Disk(DiskIdentifier(id), capacity)
        }

        fun flashDiskOf(id: String): Disk? =
            DiskUtil.disks.find { it.id.name == id } ?: throw IllegalArgumentException("$id does not exist.")
    }
}

/**
 * Flashes a disk using [file]. If specified [disk] (e.g. `disk2`) is used.
 * Otherwise any available physical removable disk is used iff there is exactly one candidate.
 */
fun RenderingLogger.flash(file: Path, disk: String?): Disk? =
    logging("Flashing ${file.fileName} (${file.size})", bordered = false) {

        val disks = listDisks()

        val flashDisk: Disk? = disk?.let {
            disks.singleOrNull { it.id.name == disk }
        } ?: disks.singleOrNull()

        flashDisk?.run { flash(file) } ?: run {
            logLine {
                if (disk == null) {
                    if (disks.isEmpty()) "No flash-able disks found.".bold()
                    else "Found multiple candidates: ${disks.joinToString(", ")}. Please explicitly choose one."
                } else {
                    if (disks.isEmpty()) "No disk $disk found."
                    else "Disk $disk is not among the flash-able disks: ${disks.joinToString(", ")}. Please choose another one."
                }
            }
            null
        }
    }


inline class DiskIdentifier(val name: String) {
    val device: String get() = "/dev/$name"
    override fun toString(): String = "disk \"$name\""

    companion object {
        private val diskIdentifierPattern = Regex("disk\\d+")
        fun isDisk(id: String) = diskIdentifierPattern.matches(id)
    }
}
