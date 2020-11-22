package com.bkahlert.koodies.io

import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.bufferedInputStream
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.outputStream
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Provides (de-)compression functionality for the GNU GZIP format.
 */
object GzCompressor {
    /**
     * Compresses this file using GNU ZIP.
     *
     * By default the existing file name is used and `.gz` appended.
     */
    fun Path.gzip(
        destination: Path = addExtension("gz"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true) else destination.requireExistsNot()
        bufferedInputStream().use { fileInput ->
            GZIPOutputStream(destination.outputStream()).use { gzipOutput ->
                fileInput.copyTo(gzipOutput)
            }
        }
        return destination
    }

    /**
     * Decompresses this GNU ZIP compressed file.
     *
     * By default the existing file name is used with the `.gz` suffix removed.
     */
    fun Path.gunzip(
        destination: Path = removeExtension("gz"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true) else destination.requireExistsNot()
        GZIPInputStream(bufferedInputStream()).use { gzipInput ->
            destination.outputStream().use { fileOutput ->
                gzipInput.copyTo(fileOutput)
            }
        }
        return destination
    }
}
