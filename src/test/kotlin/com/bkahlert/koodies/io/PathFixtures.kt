package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Compressor.compress
import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.delete
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory

object PathFixtures {
    fun singleFile() = Paths.tempDir().run {
        ClassPath("example.html").copyTo(resolve("example.html")).deleteOnExit()
    }

    fun archiveWithSingleFile(format: String = CompressorStreamFactory.BZIP2) =
        singleFile().run {
            val archive = compress(format, overwrite = true).deleteOnExit()
            delete(true)
            archive
        }

    fun directoryWithTwoFiles() = Paths.tempDir().apply {
        ClassPath("example.html").copyTo(resolve("example.html"))
        ClassPath("config.txt").copyTo(resolve("sub-dir/config.txt"))
        deleteOnExit()
    }

    fun archiveWithTwoFiles(format: String = ArchiveStreamFactory.ZIP) =
        directoryWithTwoFiles().run {
            val archive = archive(format, overwrite = true).deleteOnExit()
            delete(true)
            archive
        }
}
