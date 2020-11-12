package com.imgcstmzr.guestfish

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.GzCompressor.gunzip
import com.bkahlert.koodies.nio.file.conditioned
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.colors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightYellow
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.unit.BinaryPrefix
import com.bkahlert.koodies.unit.Gibi
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import com.imgcstmzr.guestfish.Guestfish.Companion.DOCKER_MOUNT_ROOT
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.singleLineLogger
import com.imgcstmzr.runtime.log.subLogger
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.copyTo
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.quoted
import com.imgcstmzr.util.removeExtension
import java.net.URI
import java.nio.file.Path
import kotlin.math.ceil

object ImageBuilder {

    private fun Size.round(): Size {
        val toString = toString(BinaryPrefix.Mebi, 1)
        val mb = toString.split(" ")[0]
        val roundedMb = ceil(mb.toDouble())
        return roundedMb.Mebi.bytes
    }

    fun Size.format(): String {
        val toString = round().toString(BinaryPrefix.Mebi, 0)
        return toString.split(" ")[0] + "M"
    }

    const val schema: String = "imgcstmzr"
    const val host: String = "build"

    /**
     * Dynamically creates a raw image with two partitions containing the files
     * as specified by the [uri].
     */
    fun buildFrom(uri: URI, logger: RenderingLogger<*>): Path {
        var path: List<Pair<Path, Path>>? = null
        logger.singleLineLogger<String>("Initiating raw image creation from $uri") {

            require(uri.scheme == schema) { "URI $uri is invalid as its scheme differs from ${schema.quoted}" }
            require(uri.host == host) { "URI $uri is invalid as its host differs from ${host.quoted}" }

            val query = mutableMapOf<String, MutableList<String>>().apply {
                uri.query.split("&").map {
                    it.split("=", limit = 2).run { first() to last() }
                }.forEach { (key, value) ->
                    computeIfAbsent(key) { mutableListOf() }.add(value)
                }
            }

            val files = query.getOrDefault("files", emptyList())
            require(query.isNotEmpty()) { "URI $uri does not reference any file using the \"files\" parameter" }

            path = files.map { it.split(">", limit = 2) }.map { relation -> relation.first().toPath() to relation.last().toPath() }
            "${path!!.size} files"
        }
        return prepareImg(logger, *path!!.toTypedArray())
    }

    private fun prepareImg(logger: RenderingLogger<*>, vararg paths: Pair<Path, Path>): Path =
        buildFrom(Paths.TEMP.resolve("imgcstmzr-" + paths.take(5).mapNotNull { it.first.fileName }.joinToString(separator = "-")).mkdirs().run {
            logger.singleLineLogger<Unit>("Copying ${paths.size} files to ${toUri()}") {
                paths.forEach { (from, to) -> from.copyTo(resolve(to).resolve(from.fileName)) }
            }
            @Suppress("SpellCheckingInspection")
            logger.singleLineLogger("Gzipping") { archive("tar", overwrite = true) }
        }, logger, totalSize = 4.Mebi.bytes, bootSize = 2.Mebi.bytes)

    /**
     * Creates an image consisting of the two partitions `root` and `boot`.
     *
     * @param archive the archive to be uncompressed to `/`
     * @param bootFileSystem the type of filesystem to use for boot
     * @param rootFileSystem the type of filesystem to use for root
     * @param totalSize the size of the disk image
     * @param bootSize the size of the boot filesystem
     * @param partitionTableType partition table type
     */
    fun buildFrom(
        archive: Path,
        logger: RenderingLogger<*>,
        bootFileSystem: FileSystemType = FileSystemType.FAT,
        rootFileSystem: FileSystemType = FileSystemType.EXT4,
        totalSize: Size = 1.Gibi.bytes,
        bootSize: Size = 128.Mebi.bytes,
        partitionTableType: PartitionTableType = PartitionTableType.MasterBootRecord,
    ): Path = logger.subLogger(
        caption = "${Now.emoji} Preparing raw image using the content of ${archive.fileName}. This takes a moment...",
        ansiCode = gray,
        borderedOutput = false,
    ) {
        val tarball = if (archive.conditioned.endsWith(".gz")) archive.gunzip(overwrite = true) else archive
        require("$tarball".endsWith(".tar")) { "Currently only \"tar\" and \"tar.gz\" files are supported." }

        val hostDirectory = tarball.parent
        val addFilesCommand = "-tar-in ${DOCKER_MOUNT_ROOT.resolve(tarball.fileName)} /" +
            (if (tarball.conditioned.endsWith(".tar")) "" else " compress:gzip")

        val imgName = "${tarball.fileName.removeExtension("tar")}.img".also { hostDirectory.resolve(it).delete() }
        logLine {
            val formattedTotalSize = totalSize.round().toString(BinaryPrefix::class)
            val formattedRootSize = (totalSize.round() - bootSize.round()).toString(BinaryPrefix::class)
            val formattedBootSize = bootSize.round().toString(BinaryPrefix::class)
            "Size: ${formattedTotalSize.yellow()} — ${"/".cyan()} ${formattedRootSize.brightYellow()} — ${"boot".cyan()} ${formattedBootSize.brightYellow()}"
        }

        @Suppress("SpellCheckingInspection")
        Guestfish.execute(
            containerName = Guestfish::class.simpleName + "-image-preparation---" + imgName,
            volumes = mapOf(hostDirectory to DOCKER_MOUNT_ROOT),
            options = listOf("-N",
                "${DOCKER_MOUNT_ROOT.resolve(imgName)}=bootroot:$bootFileSystem:$rootFileSystem:${totalSize.format()}:${bootSize.format()}:$partitionTableType"),
            commands = GuestfishOperation(listOf(
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

    @Suppress("unused")
    enum class PartitionTableType(val cliName: String) {
        MasterBootRecord("mbr"),
        GuidPartitionTable("gpt");

        override fun toString(): String = cliName
    }

    @Suppress("unused", "SpellCheckingInspection")
    enum class FileSystemType(val cliName: String) {
        FAT("vfat"),
        EXT4("ext4");

        override fun toString(): String = cliName
    }
}
