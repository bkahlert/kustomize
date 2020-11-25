package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Archiver.listArchive
import com.bkahlert.koodies.io.Archiver.unarchive
import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
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
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isLessThan
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException

@Execution(CONCURRENT)
class ArchiverTarGzTest {
    private val tempDir = tempDir().deleteOnExit()

    @TestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().tempPath().archive() },
        { tempDir.tempDir().tempPath(extension = ".tar.gz").unarchive() },
        { tempDir.tempDir().tempPath(extension = ".tar.gz").listArchive() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<NoSuchFileException>()
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.tempDir().directoryWithTwoFiles().apply { addExtension("tar").addExtension("gz").touch().writeText("content") }.archive("tar.gz") },
        { tempDir.tempDir().archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtension("gz").removeExtension("tar")) }.unarchive() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`() = listOf(
        {
            tempDir.tempDir().directoryWithTwoFiles().apply { addExtension("tar").addExtension("gz").touch().writeText("content") }
                .archive("tar.gz", overwrite = true)
        },
        { tempDir.tempDir().archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtension("gz").removeExtension("tar")) }.unarchive(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar-gzip and untar-gunzip`() {
        val dir = tempDir.directoryWithTwoFiles()

        val archivedDir = dir.archive()
        expectThat(archivedDir.size).isLessThan(dir.size)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.unarchive()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}

