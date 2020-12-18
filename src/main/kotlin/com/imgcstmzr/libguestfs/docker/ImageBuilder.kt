package com.imgcstmzr.libguestfs.docker

import com.bkahlert.koodies.concurrent.cleanUpOnShutdown
import com.bkahlert.koodies.concurrent.process.waitForTermination
import com.bkahlert.koodies.docker.DockerRunCommandLineBuilder
import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.GzCompressor.gunzip
import com.bkahlert.koodies.nio.file.Paths.Temp
import com.bkahlert.koodies.nio.file.copyToDirectory
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.hasExtension
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.shell.HereDocBuilder.hereDoc
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.colors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightYellow
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.unit.BinaryPrefix
import com.bkahlert.koodies.unit.Gibi
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.logging
import com.imgcstmzr.runtime.log.singleLineLogging
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
    fun BlockRenderingLogger.buildFrom(uri: URI): Path {
        var path: List<Pair<Path, Path>>? = null
        singleLineLogging("Initiating raw image creation from $uri") {

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
        return prepareImg(*path!!.toTypedArray())
    }

    private fun BlockRenderingLogger.prepareImg(vararg paths: Pair<Path, Path>): Path =
        buildFrom(Temp.resolve("imgcstmzr-" + paths.take(5).mapNotNull { it.first.fileName }.joinToString(separator = "-")).mkdirs().run {
            singleLineLogging("Copying ${paths.size} files to ${toUri()}") {
                paths.forEach { (from, to) -> from.copyToDirectory(resolve(to)) }
            }
            @Suppress("SpellCheckingInspection")
            singleLineLogging("Gzipping") { archive("tar", overwrite = true) }
        }, totalSize = 4.Mebi.bytes, bootSize = 2.Mebi.bytes)

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
    fun RenderingLogger.buildFrom(
        archive: Path,
        bootFileSystem: FileSystemType = FileSystemType.FAT,
        rootFileSystem: FileSystemType = FileSystemType.EXT4,
        totalSize: Size = 1.Gibi.bytes,
        bootSize: Size = 128.Mebi.bytes,
        partitionTableType: PartitionTableType = PartitionTableType.MasterBootRecord,
    ): Path = logging(
        caption = "${Now.emoji} Preparing raw image using the content of ${archive.fileName}. This takes a moment...",
        ansiCode = gray,
        borderedOutput = false,
    ) {
        val tarball = if (archive.hasExtension("gz")) archive.gunzip(overwrite = true).cleanUpOnShutdown() else archive
        require(tarball.hasExtension("tar")) { "Currently only \"tar\" and \"tar.gz\" files are supported." }

        val archiveDirectory = tarball.parent

        val imgName = "${tarball.fileName.removeExtension("tar")}.img".also {
            archiveDirectory.resolve(it).delete(false)
        }
        logLine {
            val formattedTotalSize = totalSize.round().toString<BinaryPrefix>()
            val formattedRootSize = (totalSize.round() - bootSize.round()).toString<BinaryPrefix>()
            val formattedBootSize = bootSize.round().toString<BinaryPrefix>()
            "Size: ${formattedTotalSize.yellow()} — ${"/".cyan()} ${formattedRootSize.brightYellow()} — ${"boot".cyan()} ${formattedBootSize.brightYellow()}"
        }

        @Suppress("SpellCheckingInspection")
        DockerRunCommandLineBuilder.build(LibguestfsDockerAdaptable.IMAGE) {
            redirects { +"2>&1" }
            workingDirectory(archiveDirectory.parent)
            options {
                env { "LIBGUESTFS_TRACE" to "1" }
                name { Libguestfs::class.simpleName + "-image-preparation---" + imgName }
                mounts { archiveDirectory mountAt "/shared" }
            }
            arguments {
                +"-N" + "/shared/$imgName=bootroot:$bootFileSystem:$rootFileSystem:${totalSize.format()}:${bootSize.format()}:$partitionTableType"
                +hereDoc {
                    +"mount /dev/sda2 /"
                    +"mkdir /boot"
                    +"mount /dev/sda1 /boot"
                    +"-tar-in /shared/${tarball.fileName} /" +
                        (if (tarball.hasExtension(".tar")) "" else " compress:gzip")
                }
            }
        }.execute().waitForTermination()
        logLine { "Finished test img creation." }
        archiveDirectory.resolve(imgName)
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
