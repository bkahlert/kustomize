package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Archiver.unarchive
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGunzip
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.nio.bufferedInputStream
import com.bkahlert.koodies.nio.file.listRecursively
import com.bkahlert.koodies.nio.file.requireEmpty
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import com.bkahlert.koodies.nio.outputStream
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.extension
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.removeExtension
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.nio.file.Path


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
    fun Path.archive(format: String = ArchiveStreamFactory.ZIP, destination: Path = addExtension(format)): Path =
        if (format == "tar.gz") {
            tarGzip()
        } else {
            requireExists()
            destination.requireExistsNot()
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
        destination: Path = removeExtension(extension
            ?: throw IllegalArgumentException("Cannot auto-detect the archive format due to missing file extension.")),
    ): Path =
        if ("$fileName".endsWith("tar.gz")) {
            tarGunzip()
        } else {
            requireExists()
            if (!destination.exists) destination.mkdirs()
            destination.requireEmpty()
            ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream()).use { it.unarchiveTo(destination) }
            destination
        }

    /**
     * Unarchives this archive input stream to [destination].
     */
    fun ArchiveInputStream.unarchiveTo(destination: Path) {
        var archiveEntry: ArchiveEntry?
        while (nextEntry.also {
                archiveEntry = it
            } != null) {
            val tarEntry: ArchiveEntry = archiveEntry ?: throw IllegalStateException()
            if (!canReadEntryData(archiveEntry)) {
                echo("$archiveEntry makes use on unsupported features. Skipping.")
                continue
            }
            val path: Path = destination.resolve(tarEntry.name)
            if (tarEntry.isDirectory) {
                require(path.mkdirs().exists) { "$path could not be created." }
            } else {
                require(path.parent.mkdirs().exists) { "${path.parent} could not be created." }
                copyTo(path.also { it.delete() }.outputStream())
            }
        }
    }
}
