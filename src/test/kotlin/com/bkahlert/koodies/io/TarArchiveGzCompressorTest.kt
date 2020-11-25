package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.listArchive
import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGunzip
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.copyTo
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasSameFiles
import com.imgcstmzr.util.renameTo
import com.imgcstmzr.util.touch
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isLessThan
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException

@Execution(CONCURRENT)
class TarArchiveGzCompressorTest {
    private val tempDir = tempDir().deleteOnExit()

    @TestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().tempPath().tarGzip() },
        { tempDir.tempDir().tempPath(extension = ".tar.gz").tarGunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<NoSuchFileException>()
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.tempDir().directoryWithTwoFiles().apply { addExtension("tar.gz").touch().writeText("content") }.tarGzip() },
        { tempDir.tempDir().archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtension("tar.gz")) }.tarGunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.tempDir().directoryWithTwoFiles().apply { addExtension("tar.gz").touch().writeText("content") }.tarGzip(overwrite = true) },
        { tempDir.tempDir().archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtension("tar.gz")) }.tarGunzip(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar-gzip listArchive and untar-gunzip`() {
        val dir = tempDir.directoryWithTwoFiles()

        val archivedDir = dir.tarGzip()
        expectThat(archivedDir.size).isLessThan(dir.size)

        expectThat(archivedDir.listArchive().map { it.name }).containsExactlyInAnyOrder("example.html", "sub-dir/", "sub-dir/config.txt")

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.tarGunzip()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}

