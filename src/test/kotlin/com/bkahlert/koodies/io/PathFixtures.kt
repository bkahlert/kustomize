package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Compressor.compress
import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.nio.file.tempDir
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.delete
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Path

object PathFixtures {
    fun Path.singleFile() = tempDir().run {
        ClassPath("example.html").copyTo(resolve("example.html")).deleteOnExit()
    }

    fun Path.archiveWithSingleFile(format: String = CompressorStreamFactory.BZIP2) =
        singleFile().run {
            val archive = compress(format, overwrite = true).deleteOnExit()
            delete(true)
            archive
        }

    fun Path.directoryWithTwoFiles() = tempDir().apply {
        ClassPath("example.html").copyTo(resolve("example.html")).deleteOnExit()
        ClassPath("config.txt").copyTo(resolve("sub-dir/config.txt")).deleteOnExit()
        deleteOnExit()
    }

    fun Path.archiveWithTwoFiles(format: String = ArchiveStreamFactory.ZIP) =
        directoryWithTwoFiles().run {
            val archive = archive(format, overwrite = true).deleteOnExit()
            delete(true)
            archive
        }
}
