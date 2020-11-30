package com.bkahlert.koodies.io

import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.bufferedInputStream
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.extensionOrNull
import com.bkahlert.koodies.nio.file.outputStream
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Path

/**
 * Provides (de-)compression functionality for a range of compression algorithms.
 *
 * @see CompressorStreamFactory
 */
object Compressor {
    /**
     * Compresses this file using the provided compression algorithm.
     *
     * By default the existing file name is used and the appropriate extension (e.g. `.gz` or `.bzip2`) appended.
     */
    fun Path.compress(
        format: String = CompressorStreamFactory.BZIP2,
        destination: Path = addExtension(format),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true) else destination.requireExistsNot()
        bufferedInputStream().use { inputStream ->
            CompressorStreamFactory().createCompressorOutputStream(format, destination.outputStream()).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return destination
    }

    /**
     * Decompresses this compressed file.
     *
     * By default the existing file name is used with the extension removed.
     */
    fun Path.decompress(
        format: String = extensionOrNull ?: throw IllegalArgumentException("Cannot auto-detect the compression format due to missing file extension."),
        destination: Path = removeExtension(extensionOrNull!!),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true) else destination.requireExistsNot()
        CompressorStreamFactory().createCompressorInputStream(bufferedInputStream()).use { inputStream ->
            destination.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return destination
    }
}
