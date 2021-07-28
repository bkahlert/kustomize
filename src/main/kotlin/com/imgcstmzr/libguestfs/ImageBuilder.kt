package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.ExitCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.UmountAllCommand
import com.imgcstmzr.libguestfs.ImageBuilder.FileSystemType.EXT4
import com.imgcstmzr.libguestfs.ImageBuilder.FileSystemType.FAT
import com.imgcstmzr.libguestfs.ImageBuilder.PartitionTableType.MasterBootRecord
import koodies.docker.DockerContainer.Companion
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.MountOptions
import koodies.io.compress.GzCompressor.gunzip
import koodies.io.path.delete
import koodies.io.path.deleteOnExit
import koodies.io.path.hasExtensions
import koodies.io.path.removeExtensions
import koodies.io.path.uriString
import koodies.shell.HereDoc
import koodies.shell.ShellScript
import koodies.text.ANSI.Text.Companion.ansi
import koodies.time.Now
import koodies.tracing.rendering.Styles.Dotted
import koodies.tracing.spanning
import koodies.unit.BinaryPrefixes
import koodies.unit.Gibi
import koodies.unit.Mebi
import koodies.unit.Size
import koodies.unit.bytes
import java.nio.file.Path
import kotlin.math.ceil

@Deprecated("very probable no more needed (only by ArchLinux because its not provided as img file)")
object ImageBuilder {

    private fun Size.round(): Size {
        val toString = toString(BinaryPrefixes.Mebi, 1)
        val mb = toString.split(" ")[0]
        val roundedMb = ceil(mb.toDouble())
        return roundedMb.Mebi.bytes
    }

    fun Size.format(): String {
        val toString = round().toString(BinaryPrefixes.Mebi, 0)
        return toString.split(" ")[0] + "M"
    }

    const val schema: String = "imgcstmzr"
    const val host: String = "build"


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
    @Deprecated("very probable no more needed (only by ArchLinux because its not provided as img file)")
    fun buildFrom(
        archive: Path,
        bootFileSystem: FileSystemType = FAT,
        rootFileSystem: FileSystemType = EXT4,
        totalSize: Size = 1.Gibi.bytes,
        bootSize: Size = 128.Mebi.bytes,
        partitionTableType: PartitionTableType = MasterBootRecord,
    ): Path = spanning("${Now.emoji} Preparing raw image using the content of ${archive.uriString}. This takes a moment …",
        decorationFormatter = { it.ansi.gray.done },
        style = Dotted) {
        val tarball = if (archive.hasExtensions("gz")) archive.gunzip(overwrite = true).deleteOnExit() else archive
        require(tarball.hasExtensions("tar")) { "Currently only \"tar\" and \"tar.gz\" files are supported." }

        val archiveDirectory = tarball.parent

        val imgName = "${tarball.fileName.removeExtensions("tar")}.img".also {
            archiveDirectory.resolve(it).delete()
        }
        log(run {
            val formattedTotalSize = totalSize.round().toString(BinaryPrefixes)
            val formattedRootSize = (totalSize.round() - bootSize.round()).toString(BinaryPrefixes)
            val formattedBootSize = bootSize.round().toString(BinaryPrefixes)
            "Size: ${formattedTotalSize.ansi.yellow} — ${"/".ansi.cyan} ${formattedRootSize.ansi.brightYellow} — ${"boot".ansi.cyan} ${formattedBootSize.ansi.brightYellow}"
        })

        DockerRunCommandLine(LibguestfsImage, Options(
            name = Companion.from("libguestfs-image-preparation---$imgName"),
            mounts = MountOptions { archiveDirectory mountAt "/shared" },
        ), ShellScript {
            line(
                "guestfish",
                "-N",
                "/shared/$imgName=bootroot:$bootFileSystem:$rootFileSystem:${totalSize.format()}:${bootSize.format()}:$partitionTableType",
                HereDoc {
                    +"mount /dev/sda2 /"
                    +"mkdir /boot"
                    +"mount /dev/sda1 /boot"
                    +"-tar-in /shared/${tarball.fileName} /" +
                        (if (tarball.hasExtensions(".tar")) "" else " compress:gzip")
                    +UmountAllCommand.joinToString(" ")
                    +ExitCommand.joinToString(" ")
                }
            )
        }).exec.logging(archiveDirectory.parent)
        log("Finished test img creation.")
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
