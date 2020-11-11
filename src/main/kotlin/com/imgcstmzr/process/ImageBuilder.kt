package com.imgcstmzr.process

import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.colors.gray
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.unit.BinaryPrefix
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.subLogger
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.removeExtension
import java.nio.file.Path
import kotlin.math.ceil
import kotlin.math.round

object ImageBuilder {

    fun Size.round(): Size {
        val toString = toString(BinaryPrefix.Mebi, 1)
        val mb = toString.split(" ")[0]
        val roundedMb = ceil(mb.toDouble())
        return roundedMb.Mebi.bytes
    }

    fun Size.format(): String {
        val toString = round().toString(BinaryPrefix.Mebi, 0)
        return toString.split(" ")[0] + "M"
    }

    fun Size.toSectors(sectorSize: Int = 512): Int = (round().bytes / sectorSize.toBigDecimal()).toInt()

    fun Size.toPartitions(sectorSize: Int = 512, ratio: Double, vararg ratios: Double): List<IntRange> {
        val partitionTableSectorCount = 4 * sectorSize
        val availableSectors = toSectors(sectorSize) - 2 * partitionTableSectorCount
        val sectors = (doubleArrayOf(ratio) + ratios).map { r ->
            round(availableSectors.toDouble() * r).toInt().also {
                require(it > 0) { "$r is too small since it leads to non-positive sector $it" }
            }
        }
        var currentSector: Int = partitionTableSectorCount
        return sectors.map { length ->
            val start = currentSector
            val end = currentSector + length
            currentSector = end
            start until end
        }
    }

    fun buildFrom(
        bootDirectory: Path,
        logger: RenderingLogger<*>,
        freeSpaceRatio: Double = 0.2,
        ratio: Pair<Double, FileSystemType> = 0.925 to FileSystemType.EXT4,
        vararg ratios: Pair<Double, FileSystemType> = arrayOf(0.075 to FileSystemType.FAT),
    ): Path = logger.subLogger(
        caption = "${Now.emoji} Preparing raw image using the content of ${bootDirectory.fileName}. This takes a moment...",
        ansiCode = gray,
    ) {
        require("$bootDirectory".endsWith(".tar.gz")) { "Currently only tar.gz files are supported." }
        require(ratio.second == FileSystemType.EXT4) { "Currently only ${FileSystemType.EXT4} is supported as the main partition." }
        require(ratios.size == 1) { "Currently only one further partition is supported." }
        require(ratios.first().second == FileSystemType.FAT) { "Currently only ${FileSystemType.FAT} is supported as the boot partition." }

        require(freeSpaceRatio >= 0) { "Free space ration must be at least 0.0" }
        val hostDirectory = bootDirectory.parent
        val addFilesCommand = "-tar-in ${Guestfish.DOCKER_MOUNT_ROOT.resolve(bootDirectory.fileName)} / compress:gzip"

        val imgName = "${bootDirectory.fileName.removeExtension("tar.gz")}.img".also { hostDirectory.resolve(it).delete() }
        val supposedCompressionFactor = 2.5
        val size = (bootDirectory.size * supposedCompressionFactor) * (1.0 + freeSpaceRatio)
        val partitions = size
            .toPartitions(ratio = ratio.first, ratios = ratios.map { it.first }.toDoubleArray())
            .zip(listOf(ratio.second) + ratios.map { it.second })

        logLine { "${partitions.size} partitions with sectors: ${partitions.map { it.first }.joinToString(", ")}" }
        logLine { "Compressed size: ${bootDirectory.size.toString(BinaryPrefix::class)}" }
        logLine { "Final image size: ${size.round().toString(BinaryPrefix::class)}" }

        Guestfish.execute(
            containerName = Guestfish::class.simpleName + "-image-preparation---" + imgName,
            volumes = mapOf(hostDirectory to Guestfish.DOCKER_MOUNT_ROOT),
            GuestfishOperation(listOf(
                "sparse ${Guestfish.DOCKER_MOUNT_ROOT.resolve(imgName)} ${size.format()}", // e.g. 4M
                "run",
                "part-init /dev/sda mbr",
                "echo \"num sectors:\"",
                "blockdev-getsz /dev/sda",
//                *partitions.map { "part-add /dev/sda p ${it.first.start} ${it.first.endInclusive}" }.toTypedArray(),
                "part-add /dev/sda p 2048 ${partitions.first().first.last - 1}",
                "part-add /dev/sda p ${partitions.first().first.last} -2048",

//                *partitions.mapIndexed { i, it -> "mkfs ${it.second.mkfsName} /dev/sda${i + 1}" }.toTypedArray(),
                "mkfs vfat /dev/sda1",
                "mkfs vfat /dev/sda2",

                "mount /dev/sda2 /",
                "mkdir /boot",
                "mount /dev/sda1 /boot",
                addFilesCommand,
            )),
            logger = this,
        )
        logLine { "Finished test img creation." }
        hostDirectory.resolve(imgName)
    }

    enum class FileSystemType(val mkfsName: String) {
        FAT("vfat"),
        EXT4("ext4")
    }
}
