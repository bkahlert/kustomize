package com.imgcstmzr.libguestfs.docker

import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import koodies.docker.DockerCommandLineBuilder
import koodies.io.compress.Archiver.archive
import koodies.io.compress.GzCompressor.gunzip
import koodies.io.path.Locations
import koodies.io.path.asPath
import koodies.io.path.copyToDirectory
import koodies.io.path.delete
import koodies.io.path.hasExtensions
import koodies.io.path.removeExtensions
import koodies.logging.RenderingLogger
import koodies.logging.compactLogging
import koodies.logging.logging
import koodies.runtime.deleteOnExit
import koodies.shell.HereDocBuilder.hereDoc
import koodies.terminal.AnsiCode.Companion.colors.gray
import koodies.terminal.AnsiColors.brightYellow
import koodies.terminal.AnsiColors.cyan
import koodies.terminal.AnsiColors.yellow
import koodies.text.Unicode.Emojis.emoji
import koodies.text.quoted
import koodies.time.Now
import koodies.unit.BinaryPrefix
import koodies.unit.Gibi
import koodies.unit.Mebi
import koodies.unit.Size
import koodies.unit.bytes
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories
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
    fun RenderingLogger.buildFrom(uri: URI): Path {
        var path: List<Pair<Path, Path>>? = null
        compactLogging("Initiating raw image creation from $uri") {

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

            path = files.map { it.split(">", limit = 2) }
                .map { relation -> relation.first().asPath() to relation.last().asPath() }
            "${path!!.size} files"
        }
        return prepareImg(*path!!.toTypedArray())
    }

    private fun RenderingLogger.prepareImg(vararg paths: Pair<Path, Path>): Path =
        buildFrom(Locations.Temp.resolve("imgcstmzr-" + paths.take(5).mapNotNull { it.first.fileName }
            .joinToString(separator = "-")).createDirectories().run {
            compactLogging("Copying ${paths.size} files to ${toUri()}") {
                paths.forEach { (from, to) -> from.copyToDirectory(resolve(to)) }
            }
            @Suppress("SpellCheckingInspection")
            compactLogging("Gzipping") { archive("tar", overwrite = true) }
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
        bordered = false,
    ) {
        val tarball = if (archive.hasExtensions("gz")) archive.gunzip(overwrite = true).deleteOnExit() else archive
        require(tarball.hasExtensions("tar")) { "Currently only \"tar\" and \"tar.gz\" files are supported." }

        val archiveDirectory = tarball.parent

        val imgName = "${tarball.fileName.removeExtensions("tar")}.img".also {
            archiveDirectory.resolve(it).delete()
        }
        logLine {
            val formattedTotalSize = totalSize.round().toString<BinaryPrefix>()
            val formattedRootSize = (totalSize.round() - bootSize.round()).toString<BinaryPrefix>()
            val formattedBootSize = bootSize.round().toString<BinaryPrefix>()
            "Size: ${formattedTotalSize.yellow()} — ${"/".cyan()} ${formattedRootSize.brightYellow()} — ${"boot".cyan()} ${formattedBootSize.brightYellow()}"
        }

        @Suppress("SpellCheckingInspection")
        DockerCommandLineBuilder.build(LibguestfsCommandLine.DOCKER_IMAGE) {
            options {
                name { Libguestfs::class.simpleName + "-image-preparation---" + imgName }
                mounts { archiveDirectory mountAt "/shared" }
            }
            commandLine {
                redirects { +"2>&1" }
                environment { "LIBGUESTFS_TRACE" to "1" }
                workingDirectory { archiveDirectory.parent }
                arguments {
                    +"-N" + "/shared/$imgName=bootroot:$bootFileSystem:$rootFileSystem:${totalSize.format()}:${bootSize.format()}:$partitionTableType"
                    +hereDoc {
                        +"mount /dev/sda2 /"
                        +"mkdir /boot"
                        +"mount /dev/sda1 /boot"
                        +"-tar-in /shared/${tarball.fileName} /" +
                            (if (tarball.hasExtensions(".tar")) "" else " compress:gzip")
                    }
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
