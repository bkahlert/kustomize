package com.bkahlert.koodies.io

import com.bkahlert.koodies.nio.bufferedInputStream
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import com.bkahlert.koodies.nio.outputStream
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.removeExtension
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
    fun Path.gzip(destination: Path = addExtension("gz")): Path {
        requireExists()
        destination.requireExistsNot()
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
    fun Path.gunzip(destination: Path = removeExtension("gz")): Path {
        requireExists()
        destination.requireExistsNot()
        GZIPInputStream(bufferedInputStream()).use { gzipInput ->
            destination.outputStream().use { fileOutput ->
                gzipInput.copyTo(fileOutput)
            }
        }
        return destination
    }
}
