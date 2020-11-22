package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.addToArchive
import com.bkahlert.koodies.io.Archiver.list
import com.bkahlert.koodies.io.Archiver.unarchiveTo
import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.bufferedInputStream
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.outputStream
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.requireEmpty
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.nio.file.Path

/**
 * Provides (de-)compression and (un-)archiving functionality for the TAR archive format compressed using the GNU GZIP format.
 */
object TarArchiveGzCompressor {
    /**
     * Archives this directory using the TAR archive format and compresses the archive using GNU ZIP.
     *
     * By default the existing file name is used and `.tar.gz` appended.
     */
    fun Path.tarGzip(
        destination: Path = addExtension("tar.gz"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true) else destination.requireExistsNot()
        GzipCompressorOutputStream(destination.outputStream()).use { gzipOutput ->
            TarArchiveOutputStream(gzipOutput).use { addToArchive(it) }
        }
        return destination
    }

    /**
     * Decompresses this GNU ZIP compressed file and unarchives the decompressed TAR archive.
     *
     * By default the existing file name is used with the `.tar.gz` suffix removed.
     */
    fun Path.tarGunzip(
        destination: Path = removeExtension("tar.gz"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true)
        if (!destination.exists) destination.mkdirs()
        if (!overwrite) destination.requireEmpty()
        GzipCompressorInputStream(bufferedInputStream()).use { gzipInput ->
            TarArchiveInputStream(gzipInput).use { it.unarchiveTo(destination) }
        }
        return destination
    }

    /**
     * Lists this archive without unarchiving it.
     */
    fun Path.listArchive(): List<ArchiveEntry> {
        requireExists()
        var archiveEntries: List<ArchiveEntry>
        GzipCompressorInputStream(bufferedInputStream()).use { gzipInput ->
            TarArchiveInputStream(gzipInput).use { archiveEntries = it.list() }
        }
        return archiveEntries
    }
}
