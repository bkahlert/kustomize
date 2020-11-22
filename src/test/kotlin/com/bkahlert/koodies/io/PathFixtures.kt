package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Compressor.compress
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.list
import com.bkahlert.koodies.nio.file.tempDir
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.ImgFixture.Boot.ConfigTxt
import com.imgcstmzr.util.ImgFixture.Home.User.ExampleHtml
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Path

// TODO provide more neutral test data (e.g. FixturePath61C285F09D95930D0AE298B00AF09F918B0A)
object PathFixtures {
    fun Path.singleFile(): Path = ExampleHtml.copyToDirectory(this).deleteOnExit()
        .apply { check(exists) { "Failed to provide archive with single file." } }

    fun Path.archiveWithSingleFile(format: String = CompressorStreamFactory.BZIP2): Path =
        singleFile().run {
            val archive = compress(format, overwrite = true).deleteOnExit()
            delete()
            archive
        }.apply { check(exists) { "Failed to provide archive with single file." } }

    fun Path.directoryWithTwoFiles(): Path = tempDir().also {
        ExampleHtml.copyToDirectory(it)
        ConfigTxt.copyToDirectory(it.resolve("sub-dir"))
        deleteOnExit()
    }.apply { check(list().count() == 2) { "Failed to provide directory with two files." } }

    fun Path.archiveWithTwoFiles(format: String = ArchiveStreamFactory.ZIP): Path =
        directoryWithTwoFiles().run {
            val archive = archive(format, overwrite = true).deleteOnExit()
            delete(true)
            archive
        }.apply { check(exists) { "Failed to provide directory with two files." } }
}
