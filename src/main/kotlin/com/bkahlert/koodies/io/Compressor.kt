package com.bkahlert.koodies.io

import com.bkahlert.koodies.nio.bufferedInputStream
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import com.bkahlert.koodies.nio.outputStream
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.extension
import com.imgcstmzr.util.removeExtension
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Path

/**
 * Provides (de-)compression functionality for a range of compression algorithms.
 *
 * @see CompressorStreamFactory
 */
object Compressor {
    /**
     * Compresses this directory using the provided compression algorithm.
     *
     * By default the existing file name is used and the appropriate extension (e.g. `.gz` or `.bzip2`) appended.
     */
    fun Path.compress(format: String = CompressorStreamFactory.BZIP2, destination: Path = addExtension(format)): Path {
        requireExists()
        destination.requireExistsNot()
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
        format: String = extension ?: throw IllegalArgumentException("Cannot auto-detect the compression format due to missing file extension."),
        destination: Path = removeExtension(extension!!),
    ): Path {
        requireExists()
        destination.requireExistsNot()
        CompressorStreamFactory().createCompressorInputStream(bufferedInputStream()).use { inputStream ->
            destination.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return destination
    }
}
