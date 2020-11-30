package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Archiver.unarchive
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGunzip
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.bufferedInputStream
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.extensionOrNull
import com.bkahlert.koodies.nio.file.hasExtension
import com.bkahlert.koodies.nio.file.listRecursively
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.outputStream
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.requireEmpty
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.util.isFile
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.nio.file.Path
import com.bkahlert.koodies.io.TarArchiveGzCompressor.listArchive as tarGzListArchive

/**
 * Provides (un-)archiving functionality for a range of archive formats.
 *
 * For the sake of convenience [archive] and [unarchive] also handle `tar.gz` files.
 *
 * @see ArchiveStreamFactory
 */
object Archiver {
    /**
     * Archives this directory using the provided archive format.
     *
     * By default the existing file name is used and the appropriate extension (e.g. `.tar` or `.zip`) appended.
     */
    fun Path.archive(
        format: String = ArchiveStreamFactory.ZIP,
        destination: Path = addExtension(format),
        overwrite: Boolean = false,
    ): Path =
        if (format == "tar.gz") {
            tarGzip(destination, overwrite = overwrite)
        } else {
            requireExists()
            if (overwrite) destination.delete(true) else destination.requireExistsNot()
            ArchiveStreamFactory().createArchiveOutputStream(format, destination.outputStream()).use { addToArchive(it) }
            destination
        }

    /**
     * Archives this directory by adding each entry to the [archiveOutputStream].
     */
    fun Path.addToArchive(archiveOutputStream: ArchiveOutputStream) {
        listRecursively().forEach { path ->
            val entryName = "${relativize(path)}"
            val entry: ArchiveEntry = archiveOutputStream.createArchiveEntry(path.toFile(), entryName)
            archiveOutputStream.putArchiveEntry(entry)
            if (path.isFile) path.bufferedInputStream().copyTo(archiveOutputStream)
            archiveOutputStream.closeArchiveEntry()
        }
    }

    /**
     * Unarchives this archive.
     *
     * By default the existing file name is used with the extension removed.
     */
    fun Path.unarchive(
        destination: Path = if (hasExtension("tar.gz")) removeExtension("tar.gz") else extensionOrNull?.let { removeExtension(it) }
            ?: throw IllegalArgumentException("Cannot auto-detect the archive format due to missing file extension."),
        overwrite: Boolean = false,
    ): Path =
        if (hasExtension("tar.gz")) {
            tarGunzip(destination, overwrite = overwrite)
        } else {
            requireExists()
            if (overwrite) destination.delete(true)
            if (!destination.exists) destination.mkdirs()
            if (!overwrite) destination.requireEmpty()
            ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream()).use { it.unarchiveTo(destination) }
            destination
        }

    /**
     * Unarchives this archive input stream to [destination].
     */
    fun ArchiveInputStream.unarchiveTo(
        destination: Path,
    ) {
        var archiveEntry: ArchiveEntry?
        while (nextEntry.also { archiveEntry = it } != null) {
            if (!canReadEntryData(archiveEntry)) {
                echo("$archiveEntry makes use on unsupported features. Skipping.")
                continue
            }
            val path: Path = destination.resolve(archiveEntry!!.name)
            if (archiveEntry!!.isDirectory) {
                require(path.mkdirs().exists) { "$path could not be created." }
            } else {
                require(path.parent.mkdirs().exists) { "${path.parent} could not be created." }
                path.delete().outputStream().also { copyTo(it) }.also { it.close() }
            }
        }
    }

    /**
     * Lists this archive input stream.
     */
    fun ArchiveInputStream.list(): List<ArchiveEntry> {
        val archiveEntries = mutableListOf<ArchiveEntry>()
        var archiveEntry: ArchiveEntry?
        while (nextEntry.also { archiveEntry = it } != null) {
            archiveEntries.add(archiveEntry!!)
        }
        return archiveEntries
    }

    /**
     * Lists this archive without unarchiving it.
     */
    fun Path.listArchive(): List<ArchiveEntry> =
        if ("$fileName".endsWith("tar.gz")) {
            tarGzListArchive()
        } else {
            requireExists()
            ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream()).use { it.list() }
        }
}
